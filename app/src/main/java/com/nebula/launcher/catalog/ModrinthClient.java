package com.nebula.launcher.catalog;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal asynchronous Modrinth v2 search client. */
public final class ModrinthClient {
    private static final String API = "https://api.modrinth.com/v2/search";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static final class Project {
        public final String id, slug, title, description, projectType;
        public final long downloads;

        Project(JSONObject j) {
            id = j.optString("project_id");
            slug = j.optString("slug");
            title = j.optString("title");
            description = j.optString("description");
            projectType = j.optString("project_type");
            downloads = j.optLong("downloads");
        }
    }

    public interface Callback {
        void onResult(List<Project> projects, String error);
    }

    public void search(String query, String projectType, String gameVersion, String loader, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                if (!("mod".equals(projectType) || "modpack".equals(projectType)
                        || "shader".equals(projectType) || "resourcepack".equals(projectType))) {
                    throw new IllegalArgumentException("Tipo de contenido no válido");
                }
                if (!CompatibilityFilter.isSupportedVersion(gameVersion)) {
                    throw new IllegalArgumentException("Versión de Minecraft no válida");
                }
                if (!CompatibilityFilter.isSupportedLoader(loader)) {
                    throw new IllegalArgumentException("Cargador no válido");
                }

                JSONArray facets = new JSONArray();
                facets.put(new JSONArray().put("project_type:" + projectType));
                facets.put(new JSONArray().put("versions:" + gameVersion));

                // Forge is a loader/category filter for mods and modpacks, not
                // for shaders or resource packs.
                if (!"any".equals(loader) && ("mod".equals(projectType) || "modpack".equals(projectType))) {
                    facets.put(new JSONArray().put("categories:" + loader));
                }

                String encodedFacets = URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8.name());
                String encodedQuery = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8.name());
                URL url = new URL(API + "?query=" + encodedQuery + "&facets=" + encodedFacets + "&limit=30");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("User-Agent", "NebulaLauncher/0.1.0 (Android)");

                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Modrinth respondió HTTP " + status);
                }

                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                }

                JSONArray hits = new JSONObject(body.toString()).optJSONArray("hits");
                List<Project> result = new ArrayList<>();
                if (hits != null) {
                    for (int i = 0; i < hits.length(); i++) result.add(new Project(hits.getJSONObject(i)));
                }
                callback.onResult(result, null);
            } catch (Exception error) {
                String message = error.getMessage();
                callback.onResult(new ArrayList<>(), message == null ? "Error de conexión" : message);
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
