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
import java.util.concurrent.RejectedExecutionException;

/** Asynchronous Modrinth v2 catalog and project-detail client. */
public final class ModrinthClient implements AutoCloseable {
    private static final String API = "https://api.modrinth.com/v2";
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread thread = new Thread(r, "Nebula-Modrinth");
        thread.setDaemon(true);
        return thread;
    });

    public static final class Project {
        public final String id, slug, title, description, projectType, iconUrl, author;
        public final long downloads;
        Project(JSONObject json) {
            id = json.optString("project_id", json.optString("id", ""));
            slug = json.optString("slug", "");
            title = json.optString("title", json.optString("name", "Proyecto sin nombre"));
            description = json.optString("description", "");
            projectType = json.optString("project_type", "");
            iconUrl = json.optString("icon_url", "");
            author = json.optString("author", "");
            downloads = Math.max(0L, json.optLong("downloads", 0L));
        }
    }

    public static final class GalleryImage {
        public final String url, title, description;
        public final boolean featured;
        GalleryImage(JSONObject json) {
            url = json.optString("url", "");
            title = json.optString("title", "");
            description = json.optString("description", "");
            featured = json.optBoolean("featured", false);
        }
    }

    public static final class Dependency {
        public final String projectId, versionId, type;
        public String title = "";
        Dependency(JSONObject json) {
            projectId = json.optString("project_id", "");
            versionId = json.optString("version_id", "");
            type = json.optString("dependency_type", "required");
        }
        public boolean isRequired() { return "required".equals(type); }
    }

    public static final class ProjectDetails {
        public final Project project;
        public final String body, team;
        public final List<String> authors = new ArrayList<>();
        public final List<GalleryImage> gallery = new ArrayList<>();
        public final List<Dependency> dependencies = new ArrayList<>();
        ProjectDetails(JSONObject json) {
            project = new Project(json);
            body = json.optString("body", "");
            team = json.optString("team", "");
            JSONArray images = json.optJSONArray("gallery");
            if (images != null) for (int i=0;i<images.length();i++) {
                JSONObject item=images.optJSONObject(i);
                if(item!=null) { GalleryImage image=new GalleryImage(item); if(!image.url.isEmpty()) gallery.add(image); }
            }
            JSONArray deps = json.optJSONArray("dependencies");
            if (deps != null) for (int i=0;i<deps.length();i++) {
                JSONObject item=deps.optJSONObject(i);
                if(item!=null) dependencies.add(new Dependency(item));
            }
        }
    }

    public interface Callback { void onResult(List<Project> projects, int totalHits, String error); }
    public interface DetailsCallback { void onResult(ProjectDetails details, String error); }

    public void search(String query, String projectType, String gameVersion, String loader,
                       int offset, Callback callback) {
        if (callback == null) return;
        try { executor.execute(() -> performSearch(query, projectType, gameVersion, loader, offset, callback)); }
        catch (RejectedExecutionException ignored) { callback.onResult(new ArrayList<>(),0,"El catálogo se está cerrando."); }
    }

    private void performSearch(String query, String projectType, String gameVersion, String loader,
                               int offset, Callback callback) {
        HttpURLConnection connection = null;
        try {
            if (!("mod".equals(projectType)||"modpack".equals(projectType)||"shader".equals(projectType)||"resourcepack".equals(projectType)))
                throw new IllegalArgumentException("Tipo de contenido no válido");
            if (!CompatibilityFilter.isSupportedVersion(gameVersion)) throw new IllegalArgumentException("Versión de Minecraft no válida");
            if (!CompatibilityFilter.isSupportedLoader(loader)) throw new IllegalArgumentException("Cargador no válido");
            if (query != null && query.length() > 200) throw new IllegalArgumentException("La búsqueda no puede superar 200 caracteres");
            if (offset < 0 || offset > 10000) throw new IllegalArgumentException("Página del catálogo fuera de rango");
            JSONArray facets=new JSONArray();
            facets.put(new JSONArray().put("project_type:"+projectType));
            facets.put(new JSONArray().put("versions:"+gameVersion));
            if(!"any".equals(loader)&&("mod".equals(projectType)||"modpack".equals(projectType)))
                facets.put(new JSONArray().put("categories:"+loader));
            String encodedFacets=URLEncoder.encode(facets.toString(),StandardCharsets.UTF_8.name());
            String encodedQuery=URLEncoder.encode(query==null?"":query.trim(),StandardCharsets.UTF_8.name());
            JSONObject response=getJson(API+"/search?query="+encodedQuery+"&facets="+encodedFacets+"&limit=30&offset="+Math.max(0,offset));
            JSONArray hits=response.optJSONArray("hits");
            List<Project> projects=new ArrayList<>();
            if(hits!=null) for(int i=0;i<hits.length();i++){JSONObject item=hits.optJSONObject(i);if(item!=null)projects.add(new Project(item));}
            callback.onResult(projects,response.optInt("total_hits",projects.size()),null);
        } catch(Exception error){callback.onResult(new ArrayList<>(),0,error.getMessage()==null?"Error de conexión con Modrinth":error.getMessage());}
        finally{if(connection!=null)connection.disconnect();}
    }

    public void getProject(String projectId, DetailsCallback callback) {
        if(callback==null)return;
        try { executor.execute(() -> {
            try {
                if(projectId==null||!projectId.matches("[A-Za-z0-9_-]{3,64}")) throw new IllegalArgumentException("Identificador de proyecto no válido");
                JSONObject json=getJson(API+"/project/"+projectId);
                ProjectDetails details=new ProjectDetails(json);
                for (Dependency dependency : details.dependencies) {
                    String dependencyProjectId = dependency.projectId;
                    if (dependencyProjectId.isEmpty() && !dependency.versionId.isEmpty()
                            && dependency.versionId.matches("[A-Za-z0-9_-]{3,64}")) {
                        try { dependencyProjectId = getJson(API + "/version/" + dependency.versionId).optString("project_id", ""); }
                        catch (Exception ignored) { /* Keep the version identifier as a fallback label. */ }
                    }
                    if (!dependencyProjectId.isEmpty() && dependencyProjectId.matches("[A-Za-z0-9_-]{3,64}")) {
                        try { dependency.title = getJson(API + "/project/" + dependencyProjectId).optString("title", dependencyProjectId); }
                        catch (Exception ignored) { dependency.title = dependencyProjectId; }
                    }
                }
                if(!details.team.isEmpty()) {
                    try {
                        JSONArray members=getArray(API+"/team/"+details.team);
                        for(int i=0;i<members.length();i++){
                            JSONObject member=members.optJSONObject(i);
                            if(member!=null){
                                JSONObject user=member.optJSONObject("user");
                                String username=user==null?"":user.optString("username","");
                                if(!username.isEmpty()&&!details.authors.contains(username))details.authors.add(username);
                            }
                        }
                    } catch(Exception ignored) {
                        // Keep project description, gallery, and dependencies available if team metadata fails.
                    }
                }
                callback.onResult(details,null);
            } catch(Exception error){callback.onResult(null,error.getMessage()==null?"No se pudieron cargar los detalles":error.getMessage());}
        }); } catch(RejectedExecutionException ignored){callback.onResult(null,"El catálogo se está cerrando.");}
    }

    private JSONObject getJson(String address) throws Exception {
        return new JSONObject(readResponse(address));
    }

    private JSONArray getArray(String address) throws Exception {
        return new JSONArray(readResponse(address));
    }

    /** Shared bounded HTTP reader prevents unexpectedly large API responses consuming heap. */
    private String readResponse(String address) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(12000);
            connection.setUseCaches(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("User-Agent", "NebulaLauncher/0.3.2 (Android)");

            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException(httpError(status));
            }

            int declaredLength = connection.getContentLength();
            if (declaredLength > MAX_RESPONSE_BYTES) {
                throw new IllegalStateException("La respuesta del catálogo es demasiado grande.");
            }

            StringBuilder body = new StringBuilder(Math.max(256,
                    declaredLength > 0 ? declaredLength : 1024));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8), 8192)) {
                char[] buffer = new char[4096];
                int count;
                int total = 0;
                while ((count = reader.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_RESPONSE_BYTES) {
                        throw new IllegalStateException("La respuesta del catálogo supera el límite permitido.");
                    }
                    body.append(buffer, 0, count);
                }
            }
            return body.toString();
        } finally {
            connection.disconnect();
        }
    }

    private String httpError(int status) {
        if (status == 429) return "Modrinth está limitando las consultas. Espera un momento y vuelve a intentarlo.";
        if (status >= 500) return "El servicio de Modrinth no está disponible temporalmente (HTTP " + status + ").";
        if (status == HttpURLConnection.HTTP_NOT_FOUND) return "El proyecto o recurso ya no está disponible.";
        return "Modrinth respondió HTTP " + status;
    }

    @Override public void close(){executor.shutdownNow();}
}
