package com.nebula.launcher.catalog;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lightweight bounded image loader; no image is shown when Modrinth has no image URL. */
public final class CatalogImageLoader implements AutoCloseable {
    private final ExecutorService executor=Executors.newFixedThreadPool(3);
    public void load(String address, ImageView target, int placeholderColor) {
        if(address==null||address.isEmpty()){target.setImageDrawable(null);return;}
        target.setTag(address);
        target.setBackgroundColor(placeholderColor);
        executor.execute(()->{
            HttpURLConnection c=null;
            try{
                c=(HttpURLConnection)new URL(address).openConnection();
                c.setConnectTimeout(8000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(false);
                c.setRequestProperty("User-Agent","NebulaLauncher/0.3.0 (Android)");
                if(c.getResponseCode()!=200)return;
                Bitmap bitmap=BitmapFactory.decodeStream(c.getInputStream());
                if(bitmap!=null)target.post(()->{if(address.equals(target.getTag()))target.setImageBitmap(bitmap);});
            }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
        });
    }
    @Override public void close(){executor.shutdownNow();}
}
