package com.android.volley.core;

import android.content.Context;
import com.android.volley.ExecutorDelivery;
import com.android.volley.RequestQueue;
import com.android.volley.ResponseDelivery;
import com.android.volley.Volley;
import com.android.volley.cache.Cache;
import com.android.volley.cache.DiskCache;
import com.android.volley.cache.NoCache;
import com.android.volley.stack.HttpStack;
import java.io.File;
import java.util.concurrent.Executors;

public class RequestQueueFactory {

    public static RequestQueue getQueue(Context context, String name) {
        RequestQueue result = null;

        if (RequestOptions.DEFAULT_QUEUE.equals(name)) {
            result = getDefault(context);
        }
        if (RequestOptions.BACKGROUND_QUEUE.equals(name)) {
            result = newBackgroundQueue(context);
        }

        return result;
    }

    public static RequestQueue getDefault(Context context) {
        return Volley.newRequestQueue(context.getApplicationContext());
    }

    public static RequestQueue getImageDefault(Context context) {
        return newImageQueue(context.getApplicationContext(), null, RequestOptions.IMAGE_DEFAULT_POOL_SIZE);
    }

    public static RequestQueue getFileDefault(Context context) {
        return newFileQueue(context.getApplicationContext(), null, RequestOptions.FILE_DEFAULT_POOL_SIZE);
    }

    public static RequestQueue newBackgroundQueue(Context context) {
        return newBackgroundQueue(context, null, RequestOptions.DEFAULT_POOL_SIZE);
    }

    public static RequestQueue newBackgroundQueue(Context context, HttpStack stack, int threadPoolSize) {
        File externalStorageDirectory = new File(context.getCacheDir(), RequestOptions.REQUEST_CACHE_PATH);

        // pass Executor to constructor of ResponseDelivery object
        ResponseDelivery delivery = new ExecutorDelivery(Executors.newFixedThreadPool(threadPoolSize));
        return Volley.newRequestQueue(context, stack, new DiskCache(externalStorageDirectory), threadPoolSize, delivery);
    }

    public static RequestQueue newFileQueue(Context context, HttpStack stack, int threadPoolSize) {
        return Volley.newRequestQueue(context, stack, new NoCache(), threadPoolSize);
    }

    public static RequestQueue newImageQueue(Context context, HttpStack stack, int threadPoolSize) {
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir == null) {
            externalCacheDir = context.getCacheDir();
        }

        File externalStorageDirectory = new File(externalCacheDir, RequestOptions.IMAGE_CACHE_PATH);
        if (!externalStorageDirectory.exists()) externalStorageDirectory.mkdirs();

        Cache diskCache = new DiskCache(externalStorageDirectory, RequestOptions.DEFAULT_DISK_USAGE_BYTES);
        return Volley.newRequestQueue(context, stack, diskCache, threadPoolSize);
    }
}
