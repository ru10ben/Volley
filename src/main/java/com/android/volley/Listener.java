
package com.android.volley;

import com.android.volley.error.VolleyError;

/**
 * Callback interface for delivering request status or response result. Note :
 * all method are calls over UI thread.
 * 
 * @param <T> Parsed type of this response
 */
public abstract class Listener<T> {

    /**
     * Inform when start to handle this Request.
     */
    public void onStart() {
    }

    /**
     * Inform when {@link Request} execute is finish, whatever success or error
     * or cancel, this callback method always invoke if request is done.
     */
    public void onFinish() {
    }

    /**
     * Inform when the {@link Request} is truly cancelled.
     */
    public void onCancel() {
    }

    /**
     * Inform when {@link Request} execute is going to retry.
     */
    public void onRetry() {
    }

    /**
     * Callback method that an error has been occurred with the provided error
     * code and optional user-readable message.
     */
    public void onError(VolleyError error) {
    }

    /**
     * Inform When the {@link Request} cache non-exist or expired, this callback
     * method is opposite by the onUsedCache(), means the http retrieving will
     * happen soon.
     */
    public void onNetworking() {
    }

    /**
     * Inform when the cache already use, it means http networking won't
     * execute.
     */
    public void onUsedCache() {
    }

    /**
     * Inform when download progress change, this callback method only available
     * when request was
     */
    public void onProgressUpdate(long totalSize, long transferred) {
    }
    
    /**
     * Called when response success.
     */
    public abstract void onSuccess(T response);
}
