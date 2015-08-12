
package com.android.volley;

import java.io.File;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.pm.PackageInfo;
import com.android.volley.cache.Cache;
import com.android.volley.cache.DiskCache;
import com.android.volley.stack.HttpClientStack;
import com.android.volley.stack.HttpStack;
import com.android.volley.stack.HurlStack;
import com.android.volley.stack.OkHttpStack;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.FileDownloader;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.squareup.okhttp.OkHttpClient;

import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Volley {
    
    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";
    
    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, null);
    }
    
    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        return newRequestQueue(context, stack, new DiskCache(cacheDir));
    }
    
    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @param cache A Cache to use for persisting responses to disk
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, Cache cache) {
        return newRequestQueue(context, stack, cache, RequestQueue.DEFAULT_NETWORK_THREAD_POOL_SIZE);
    }
    
    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @param cache A Cache to use for persisting responses to disk
     * @param threadPoolSize Number of network dispatcher threads to create
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, Cache cache, 
            int threadPoolSize) {
        return newRequestQueue(context, stack, cache, threadPoolSize, 
                new ExecutorDelivery(new Handler(Looper.getMainLooper())));
    }
    
    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @param cache A Cache to use for persisting responses to disk
     * @param threadPoolSize Number of network dispatcher threads to create
     * @param delivery A Delivery interface for posting responses and errors
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack, Cache cache, 
            int threadPoolSize, ResponseDelivery delivery) {
        if (stack == null) {
            try {
                if (Class.forName("com.squareup.okhttp.OkHttpClient") != null) {
                    stack = new OkHttpStack(new OkHttpClient());
                }
            } catch (ClassNotFoundException e) {
                String userAgent=buildUserAgent(context);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    stack = new HurlStack(userAgent);
                } else {
                    stack = new HttpClientStack(userAgent);
                }
            }
        }

        RequestQueue queue = new RequestQueue(cache, new BasicNetwork(stack), threadPoolSize, delivery);
        queue.start();

        return queue;    
    }
    
    /**
     * Build UserAgent to report in your HTTP requests.
     */
    public static String buildUserAgent(Context context) {
        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (NameNotFoundException e) {
        }
        return userAgent;
    }
    
    /**
     * Creates a default instance of the ImageLoader.
     * @param queue       The RequestQueue to use for making image requests.
     * @param imageCache  The cache to use as an L1 cache.
     * @return A started {@link ImageLoader} instance.
     */
    public static ImageLoader newImageLoader(RequestQueue queue, ImageCache imageCache) {
        return new ImageLoader(queue, imageCache);
    }
    
    /**
     * Creates a default instance of the FileDownloader.
     * @param queue       The RequestQueue for dispatching Download task.
     * @param taskCount   Allows parallel task count,
     * @return A started {@link FileDownloader} instance.
     */
    public static FileDownloader newFileDownloader(RequestQueue queue, int taskCount) {
        return new FileDownloader(queue, taskCount);
    }
    
    /**
     * Toggle whether debug logging is enabled.
     */
    public static void setLoggingEnabled(boolean enabled) {
        VolleyLog.DEBUG = enabled;
    }
}
