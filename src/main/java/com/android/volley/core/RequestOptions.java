package com.android.volley.core;

public class RequestOptions {

    public static final String DEFAULT_QUEUE            = "QUEUE_DEFAULT";
    public static final String BACKGROUND_QUEUE         = "QUEUE_BACKGROUND";

    public static final String DEFAULT_FILE_LOADER      = "FILE_LOADER_DEFAULT";
    public static final String DEFAULT_IMAGE_LOADER     = "IMAGE_LOADER_DEFAULT";

    public static final int DEFAULT_POOL_SIZE           = 4;
    public static final int FILE_DEFAULT_POOL_SIZE      = 2;
    public static final int IMAGE_DEFAULT_POOL_SIZE     = 4;
    
    public static final String FILE_CACHE_PATH          = "files";
    public static final String IMAGE_CACHE_PATH         = "images";
    public static final String REQUEST_CACHE_PATH       = "request";

    public static final int DEFAULT_FILE_TASK_COUNT     = 1;
    public static final int DEFAULT_DISK_USAGE_BYTES    = 10 * 1024 * 1024;

}
