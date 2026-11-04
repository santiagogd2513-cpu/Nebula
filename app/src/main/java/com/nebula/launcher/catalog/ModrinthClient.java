package com.nebula.launcher.catalog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/** Small asynchronous Modrinth v2 search client. */
public final class ModrinthClient implements AutoCloseable {
    private static final String API = "https://api.modrinth.com/v2/search";
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Nebula-Modrinth");
        thread.setDaemon(true);
        return thread;
    });

    public static final class Project {
        public final String id;
        public final String slug;
        public final String title;
        public final String description;
        public final String projectType;
        public final long downloads;

        Project(JSONObject json) {
            id = json.optString("project_id", "");
            slug = json.optString("slug", "");
            title = json.optString("title", "Proyecto sin nombre");
            description = json.optString("description", "");
            projectType = json.optString("project_type", "");
            downloads = Math.max(0L, json.optLong("downloads", 0L));
        }
    }

    public interface Callback {
        void onResult(List<Project> projects, String error);
    }

    public void search(String query, String projectType, String gameVersion, String loader, Callback callback) {
        if (callback == null) return;
        try {
            executor.execute(() -> performSearch(query, projectType, gameVersion, loader, callback));
        } catch (RejectedExecutionException ignored) {
            callback.onResult(new ArrayList<>(), "El catálogo se está cerrando. Vuelve a abrir la sección.");
        }
    }

    private void performSearch(String query, String projectType, String gameVersion,
                               String loader, Callback callback) {
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
            if (!"any".equals(loader) && ("mod".equals(projectType) || "modpack".equals(projectType))) {
                facets.put(new JSONArray().put("categories:" + loader));
            }

            String encodedFacets = URLEncoder.encode(facets.toString(), StandardCharsets.UTF_8.name());
            String encodedQuery = URLEncoder.encode(query == null ? "" : query.trim(), StandardCharsets.UTF_8.name());
            URL url = new URL(API + "?query=" + encodedQuery + "&facets=" + encodedFacets + "&limit=30");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "NebulaLauncher/0.2.0 (Android)");

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
            List<Project> projects = new ArrayList<>();
            if (hits != null) {
                for (int i = 0; i < hits.length(); i++) {
                    JSONObject item = hits.optJSONObject(i);
                    if (item != null) projects.add(new Project(item));
                }
            }
            callback.onResult(projects, null);
        } catch (Exception error) {
            String message = error.getMessage();
            callback.onResult(new ArrayList<>(), message == null ? "Error de conexión con Modrinth" : message);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override public void close() {
        executor.shutdownNow();
    }
}
