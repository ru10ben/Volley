package com.android.volley.core;

import android.content.Context;
import com.android.volley.Listener;
import com.android.volley.RequestQueue;
import com.android.volley.cache.BitmapLruCache;
import com.android.volley.toolbox.FileDownloader;
import com.android.volley.toolbox.ImageLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * Volley核心处理类
 *
 * @author Jacky.Lee <loveselience@gmail.com>
 */
public class RequestManager {

    private static RequestManager instance;

    private RequestController mRequestController;

    private ImageLoaderController mImageLoaderController;

    private FileDownloaderController mFileDownloaderController;

    private RequestManager(Context context) {
        Context applicationContext = context.getApplicationContext();
        mRequestController = new RequestController(applicationContext);
        mImageLoaderController = new ImageLoaderController(applicationContext);
        mFileDownloaderController = new FileDownloaderController(applicationContext);
    }

    public static synchronized void initializeWith(Context context) {
        if (instance == null) {
            instance = new RequestManager(context);
        }
    }

    public static synchronized QueueBuilder queue() {
        if (instance == null) {
            throw new IllegalStateException(RequestManager.class.getSimpleName() +
                    " is not initialized, call initializeWith(..) method first.");
        }
        return instance.getRequestController().mQueueBuilder;
    }

    public static synchronized ImageQueueBuilder loader() {
        if (instance == null) {
            throw new IllegalStateException(RequestManager.class.getSimpleName() +
                    " is not initialized, call initializeWith(..) method first.");
        }
        return instance.getImageLoaderController().mImageQueueBuilder;
    }

    public static synchronized FileQueueBuilder downloader() {
        if (instance == null) {
            throw new IllegalStateException(RequestManager.class.getSimpleName() +
                    " is not initialized, call initializeWith(..) method first.");
        }
        return instance.getFileDownloaderController().mFileQueueBuilder;
    }

    private RequestController getRequestController() {
        return mRequestController;
    }

    private ImageLoaderController getImageLoaderController() {
        return mImageLoaderController;
    }

    private FileDownloaderController getFileDownloaderController() {
        return mFileDownloaderController;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public class RequestController {

        private QueueBuilder mQueueBuilder;

        public RequestController(Context context) {
            mQueueBuilder = new QueueBuilder(context);
        }

        public RequestController addRequest(RequestInterface volleyRequest,
                                            RequestCallback requestCallback) {
            volleyRequest.setRequestCallback(requestCallback);
            mQueueBuilder.getRequestQueue().add(volleyRequest.create());
            return this;
        }

        public RequestController addRequest(RequestInterface volleyRequest,
                                            Listener responseListener) {
            volleyRequest.setResponseListener(responseListener);
            mQueueBuilder.getRequestQueue().add(volleyRequest.create());
            return this;
        }

        public void start() {
            mQueueBuilder.getRequestQueue().start();
        }

        public void stop() {
            mQueueBuilder.getRequestQueue().stop();
        }

        public void cancelAll(Object tag) {
            mQueueBuilder.getRequestQueue().cancelAll(tag);
        }

        public void cancelAll(RequestQueue.RequestFilter requestFilter) {
            mQueueBuilder.getRequestQueue().cancelAll(requestFilter);
        }
    }

    public class ImageLoaderController {

        private ImageQueueBuilder mImageQueueBuilder;

        public ImageLoaderController(Context context) {
            mImageQueueBuilder = new ImageQueueBuilder(context);
        }

        public ImageLoader obtain() {
            return mImageQueueBuilder.getLoader();
        }

        public void clearCache() {
            final BitmapLruCache cache = mImageQueueBuilder.getCache();
            if (cache != null) {
                cache.evictAll();
            }
        }
    }

    public class FileDownloaderController {

        private FileQueueBuilder mFileQueueBuilder;

        public FileDownloaderController(Context context) {
            mFileQueueBuilder = new FileQueueBuilder(context);
        }

        public FileDownloader obtain() {
            return mFileQueueBuilder.getLoader();
        }
    }

    public class QueueBuilder {

        private Context mContext;

        private Map<String, RequestQueue> mRequestQueue = new HashMap<String, RequestQueue>();

        private String mCurQueue;

        public QueueBuilder(Context context) {
            mContext = context;
        }

        public RequestController use(String queueName) {
            validateQueue(queueName);
            mCurQueue = queueName;
            return mRequestController;
        }

        public RequestController useDefaultQueue() {
            return use(RequestOptions.DEFAULT_QUEUE);
        }

        public RequestController useBackgroundQueue() {
            return use(RequestOptions.BACKGROUND_QUEUE);
        }

        public void create(String queueName, RequestQueue requestQueue) {
            if (mRequestQueue.containsKey(queueName)) {
                throw new IllegalArgumentException("RequestQueue - \"" + queueName + "\" already exists!");
            }
            mRequestQueue.put(queueName, requestQueue);
        }

        private void validateQueue(String queueName) {
            if (!mRequestQueue.containsKey(queueName)) {
                final RequestQueue queue = RequestQueueFactory.getQueue(mContext, queueName);
                if (queue != null) {
                    mRequestQueue.put(queueName, queue);
                } else {
                    throw new IllegalArgumentException("RequestQueue - \"" + queueName + "\" doesn't exists!");
                }
            }
        }

        private RequestQueue getRequestQueue() {
            RequestQueue result = mRequestQueue.get(mCurQueue);
            return result;
        }
    }

    public class ImageQueueBuilder {

        private Context mContext;

        private Map<String, ImageLoader> mLoaders = new HashMap<String, ImageLoader>();
        private Map<String, BitmapLruCache> mCaches = new HashMap<String, BitmapLruCache>();

        private String mCurLoader;

        public ImageQueueBuilder(Context context) {
            mContext = context;
        }

        public ImageLoaderController use(String loaderName) {
            if (!mLoaders.containsKey(loaderName)) {
                throw new IllegalArgumentException(
                        "ImageLoader - \"" + loaderName + "\" doesn't exists!");
            }

            mCurLoader = loaderName;
            return mImageLoaderController;
        }

        public ImageLoaderController useDefaultLoader() {
            createDefaultLoader();
            mCurLoader = RequestOptions.DEFAULT_IMAGE_LOADER;
            return mImageLoaderController;
        }

        public void create(String loaderName, RequestQueue queue, BitmapLruCache bitmapLruCache) {
            if (mLoaders.containsKey(loaderName)) {
                throw new IllegalArgumentException(
                        "ImageLoader - \"" + loaderName + "\" already exists!");
            }

            mCaches.put(loaderName, bitmapLruCache);
            mLoaders.put(loaderName, ImageLoaderFactory.newLoader(queue, bitmapLruCache));
        }

        private void createDefaultLoader() {
            if (!mLoaders.containsKey(RequestOptions.DEFAULT_IMAGE_LOADER)) {

                final BitmapLruCache bitmapLruCache = new BitmapLruCache();
                final ImageLoader imageLoader = ImageLoaderFactory
                        .newLoader(mContext, bitmapLruCache);

                mLoaders.put(RequestOptions.DEFAULT_IMAGE_LOADER, imageLoader);
                mCaches.put(RequestOptions.DEFAULT_IMAGE_LOADER, bitmapLruCache);
            }
        }

        public BitmapLruCache getCache() {
            return mCaches.get(mCurLoader);
        }

        public ImageLoader getLoader() {
            return mLoaders.get(mCurLoader);
        }
    }

    public class FileQueueBuilder {

        private Context mContext;

        private Map<String, FileDownloader> mLoaders = new HashMap<String, FileDownloader>();

        private String mCurLoader;

        public FileQueueBuilder(Context context) {
            mContext = context;
        }

        public FileDownloaderController use(String loaderName) {
            if (!mLoaders.containsKey(loaderName)) {
                throw new IllegalArgumentException(
                        "FileDownloader - \"" + loaderName + "\" doesn't exists!");
            }

            mCurLoader = loaderName;
            return mFileDownloaderController;
        }

        public FileDownloaderController useDefaultLoader() {
            createDefaultLoader();
            mCurLoader = RequestOptions.DEFAULT_FILE_LOADER;
            return mFileDownloaderController;
        }

        public void create(String loaderName, RequestQueue queue, int taskCount) {
            if (mLoaders.containsKey(loaderName)) {
                throw new IllegalArgumentException(
                        "FileDownloader - \"" + loaderName + "\" already exists!");
            }

            mLoaders.put(loaderName, FileLoaderFactory.newLoader(queue, taskCount));
        }

        private void createDefaultLoader() {
            if (!mLoaders.containsKey(RequestOptions.DEFAULT_FILE_LOADER)) {

                mLoaders.put(RequestOptions.DEFAULT_FILE_LOADER, FileLoaderFactory.getDefault(mContext));
            }
        }

        public FileDownloader getLoader() {
            return mLoaders.get(mCurLoader);
        }
    }
}
