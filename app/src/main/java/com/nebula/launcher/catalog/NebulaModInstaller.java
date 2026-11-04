package com.nebula.launcher.catalog;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Installs a compatible Modrinth mod file into the currently selected Mojo engine instance. */
public final class NebulaModInstaller {
    private static final int MAX_DOWNLOAD_BYTES = 200 * 1024 * 1024;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private NebulaModInstaller() {}

    public static void install(Context context, String projectId, String minecraftVersion, String loader) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            File temporary = null;
            try {
                File gameDirectory = selectedGameDirectory();
                if (gameDirectory == null) throw new IllegalStateException("Primero abre Nebula y selecciona una instancia de Minecraft.");
                File mods = new File(gameDirectory, "mods");
                if (!mods.exists() && !mods.mkdirs()) throw new IllegalStateException("No se pudo crear la carpeta mods.");

                JSONObject version = findCompatibleVersion(projectId, minecraftVersion, loader);
                if (version == null) throw new IllegalStateException("No hay una versión compatible con Minecraft " + minecraftVersion + " y " + loader + ".");

                JSONArray files = version.getJSONArray("files");
                JSONObject selected = null;
                for (int i = 0; i < files.length(); i++) {
                    JSONObject candidate = files.getJSONObject(i);
                    String filename = candidate.optString("filename", "");
                    if (filename.toLowerCase(Locale.ROOT).endsWith(".jar")) {
                        if (selected == null || candidate.optBoolean("primary", false)) selected = candidate;
                        if (candidate.optBoolean("primary", false)) break;
                    }
                }
                if (selected == null) throw new IllegalStateException("Modrinth no proporcionó un archivo JAR instalable.");

                String filename = new File(selected.getString("filename")).getName();
                if (!filename.matches("[A-Za-z0-9._+-]{1,180}\\.jar")) throw new IllegalStateException("Nombre de archivo no válido.");
                File target = new File(mods, filename);
                temporary = new File(mods, filename + ".nebula.part");
                downloadAndVerify(selected.getString("url"), temporary, selected.optJSONObject("hashes"));
                if (target.exists() && !target.delete()) throw new IllegalStateException("No se pudo reemplazar la versión anterior del mod.");
                if (!temporary.renameTo(target)) throw new IllegalStateException("No se pudo guardar el mod en la carpeta mods.");
                temporary = null;
                show(app, "Mod instalado: " + filename);
            } catch (Exception error) {
                if (temporary != null) temporary.delete();
                show(app, "No se pudo instalar el mod: " + safeMessage(error));
            }
        }, "Nebula-Modrinth-Install").start();
        Toast.makeText(app, "Preparando instalación del mod…", Toast.LENGTH_SHORT).show();
    }

    private static File selectedGameDirectory() throws Exception {
        Class<?> instances = Class.forName("git.artdeell.mojo.instances.Instances");
        Object selected = instances.getMethod("loadSelectedInstance").invoke(null);
        if (selected == null) return null;
        return (File) selected.getClass().getMethod("getGameDirectory").invoke(selected);
    }

    private static JSONObject findCompatibleVersion(String projectId, String mc, String loader) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://api.modrinth.com/v2/project/" +
                projectId.replaceAll("[^A-Za-z0-9_-]", "") + "/version").openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "NebulaLauncher/0.3 (Android)");
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("Modrinth respondió HTTP " + connection.getResponseCode() + ".");
            byte[] data;
            try (InputStream input = connection.getInputStream(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int count, total = 0;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > 8 * 1024 * 1024) throw new IllegalStateException("La respuesta de Modrinth es demasiado grande.");
                    out.write(buffer, 0, count);
                }
                data = out.toByteArray();
            }
            JSONArray versions = new JSONArray(new String(data, java.nio.charset.StandardCharsets.UTF_8));
            String wantedLoader = loader == null ? "forge" : loader.toLowerCase(Locale.ROOT);
            for (int i = 0; i < versions.length(); i++) {
                JSONObject item = versions.getJSONObject(i);
                JSONArray gameVersions = item.optJSONArray("game_versions");
                JSONArray loaders = item.optJSONArray("loaders");
                boolean hasGame = false, hasLoader = false;
                if (gameVersions != null) for (int j = 0; j < gameVersions.length(); j++) if (mc.equals(gameVersions.optString(j))) hasGame = true;
                if (loaders != null) for (int j = 0; j < loaders.length(); j++) {
                    String candidate = loaders.optString(j).toLowerCase(Locale.ROOT);
                    if ("any".equals(wantedLoader) || wantedLoader.equals(candidate) ||
                            ("neoforge".equals(wantedLoader) && "neoforge".equals(candidate))) hasLoader = true;
                }
                if (hasGame && hasLoader) return item;
            }
            return null;
        } finally {
            connection.disconnect();
        }
    }

    private static void downloadAndVerify(String url, File target, JSONObject hashes) throws Exception {
        if (!url.startsWith("https://")) throw new IllegalStateException("La URL del archivo no es segura.");
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        try {
            if (connection.getResponseCode() != 200) throw new IllegalStateException("No se pudo descargar el archivo.");
            long declared = connection.getContentLengthLong();
            if (declared > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("El archivo supera el límite de descarga de Nebula.");
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            int total = 0;
            try (InputStream input = connection.getInputStream(); FileOutputStream output = new FileOutputStream(target)) {
                byte[] buffer = new byte[16384];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_DOWNLOAD_BYTES) throw new IllegalStateException("El archivo supera el límite de descarga de Nebula.");
                    sha1.update(buffer, 0, count);
                    output.write(buffer, 0, count);
                }
                output.getFD().sync();
            }
            String expected = hashes == null ? "" : hashes.optString("sha1", "");
            if (!expected.isEmpty()) {
                StringBuilder actual = new StringBuilder();
                for (byte b : sha1.digest()) actual.append(String.format(Locale.ROOT, "%02x", b & 0xff));
                if (!expected.equalsIgnoreCase(actual.toString())) throw new IllegalStateException("La verificación SHA-1 del archivo falló.");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String safeMessage(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private static void show(Context context, String message) {
        MAIN.post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
