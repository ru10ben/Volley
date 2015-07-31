package com.android.volley.core;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.Volley;
import com.android.volley.cache.BitmapLruCache;
import com.android.volley.toolbox.ImageLoader;

class ImageLoaderFactory {

    public static ImageLoader getDefault(Context context) {
        return newLoader(RequestQueueFactory.getImageDefault(context), new BitmapLruCache());
    }

    public static ImageLoader newLoader(Context context, int cacheSize) {
        return newLoader(RequestQueueFactory.getImageDefault(context),
                new BitmapLruCache(cacheSize));
    }

    public static ImageLoader newLoader(Context context, BitmapLruCache cache) {
        return newLoader(RequestQueueFactory.getImageDefault(context), cache);
    }

    public static ImageLoader newLoader(RequestQueue queue, BitmapLruCache cache) {
        return Volley.newImageLoader(queue, cache);
    }
}
