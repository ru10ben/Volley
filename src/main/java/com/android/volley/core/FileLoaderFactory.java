
package com.android.volley.core;

import android.content.Context;
import com.android.volley.RequestQueue;
import com.android.volley.Volley;
import com.android.volley.toolbox.FileDownloader;

public class FileLoaderFactory {
    
    public static FileDownloader getDefault(Context context) {
        return newLoader(RequestQueueFactory.getFileDefault(context),
                RequestOptions.DEFAULT_FILE_TASK_COUNT);
    }

    public static FileDownloader newLoader(Context context, int taskCount) {
        return newLoader(RequestQueueFactory.getFileDefault(context), taskCount);
    }
    
    public static FileDownloader newLoader(RequestQueue queue, int taskCount) {
        return Volley.newFileDownloader(queue, taskCount);
    }
}
