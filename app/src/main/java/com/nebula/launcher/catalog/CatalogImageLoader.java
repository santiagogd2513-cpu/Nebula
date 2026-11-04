package com.nebula.launcher.catalog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded, HTTPS-only Modrinth image loader with an in-memory bitmap cache. */
public final class CatalogImageLoader implements AutoCloseable {
    private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
    private final LruCache<String, Bitmap> cache;
    private final ThreadPoolExecutor executor;

    public CatalogImageLoader() {
        int cacheBytes = (int) Math.min(24L * 1024L * 1024L,
                Math.max(2L * 1024L * 1024L, Runtime.getRuntime().maxMemory() / 24L));
        cache = new LruCache<String, Bitmap>(Math.max(1024 * 1024, cacheBytes)) {
            @Override protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        executor = new ThreadPoolExecutor(2, 3, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32), runnable -> {
                    Thread thread = new Thread(runnable, "Nebula-Images");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    public void load(String address, ImageView target, int placeholderColor) {
        if (!isTrustedImageUrl(address)) {
            target.setTag(null);
            target.setImageDrawable(null);
            target.setBackgroundColor(placeholderColor);
            return;
        }
        target.setTag(address);
        target.setBackgroundColor(placeholderColor);
        Bitmap cached = cache.get(address);
        if (cached != null && !cached.isRecycled()) {
            target.setImageBitmap(cached);
            return;
        }
        try {
            executor.execute(() -> {
                Bitmap bitmap = downloadAndDecode(address, requestedSize(target, true), requestedSize(target, false));
                if (bitmap == null) return;
                cache.put(address, bitmap);
                target.post(() -> {
                    if (address.equals(target.getTag()) && !bitmap.isRecycled()) {
                        target.setImageBitmap(bitmap);
                    }
                });
            });
        } catch (RuntimeException ignored) {
            // A full or shutting-down image queue must not crash the launcher.
        }
    }

    private int requestedSize(ImageView target, boolean width) {
        int measured = width ? target.getWidth() : target.getHeight();
        if (measured > 0) return measured;
        android.view.ViewGroup.LayoutParams params = target.getLayoutParams();
        int requested = params == null ? 0 : (width ? params.width : params.height);
        if (requested > 0) return requested;
        return 192;
    }

    private boolean isTrustedImageUrl(String address) {
        if (address == null || address.length() > 2048) return false;
        try {
            Uri uri = Uri.parse(address);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host != null
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                    && (host.equalsIgnoreCase("modrinth.com") || host.toLowerCase(java.util.Locale.ROOT).endsWith(".modrinth.com"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private Bitmap downloadAndDecode(String address, int targetWidth, int targetHeight) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "image/*");
            connection.setRequestProperty("User-Agent", "NebulaLauncher/0.3.2 (Android)");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            int length = connection.getContentLength();
            if (length > MAX_IMAGE_BYTES) return null;
            byte[] bytes;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_IMAGE_BYTES) return null;
                    output.write(buffer, 0, count);
                }
                bytes = output.toByteArray();
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
            int width = Math.max(96, targetWidth);
            int height = Math.max(96, targetHeight);
            int sample = 1;
            while (bounds.outWidth / (sample * 2) >= width
                    && bounds.outHeight / (sample * 2) >= height) sample *= 2;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override public void close() {
        executor.shutdownNow();
        cache.evictAll();
    }
}
