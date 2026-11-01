package com.josjis.tv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(16 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount() / 1024; }
    };
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());

    public void load(ImageView view, String url) {
        if (url == null || url.isEmpty()) return;
        view.setTag(url);
        Bitmap existing = cache.get(url);
        if (existing != null) { view.setImageBitmap(existing); return; }
        pool.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                if (connection.getResponseCode() != 200) return;
                Bitmap bitmap;
                try (InputStream stream = connection.getInputStream()) { bitmap = BitmapFactory.decodeStream(stream); }
                if (bitmap != null) {
                    cache.put(url, bitmap);
                    main.post(() -> { if (url.equals(view.getTag())) view.setImageBitmap(bitmap); });
                }
            } catch (Exception ignored) { }
            finally { if (connection != null) connection.disconnect(); }
        });
    }
    public void shutdown() { pool.shutdownNow(); }
}
