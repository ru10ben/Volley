
package com.android.volley.request;

import com.android.volley.Listener;
import com.android.volley.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * A request for making a MultiPart request
 */
public abstract class MultiPartRequest<T> extends Request<T> {

    private static final String PROTOCOL_CHARSET = "UTF-8";

    /**
     * A map for uploading files
     */
    private Map<String, String> mFileUploads = null;


    public MultiPartRequest(int method, String url, Listener<T> listener) {
        super(method, url, listener);
        mFileUploads = new HashMap<String, String>();
    }

    /**
     * Add a file to be uploaded in the multipart request
     *
     * @param name     The name of the file key
     * @param filePath The path to the file. This file MUST exist.
     * @return The Multipart request for chaining method calls
     */
    public MultiPartRequest addFile(String name, String filePath) {
        mFileUploads.put(name, filePath);
        return this;
    }

    /**
     * Get all the files to be uploaded for this request
     *
     * @return A map of all the files to be uploaded for this request
     */
    public Map<String, String> getFilesToUpload() {
        return mFileUploads;
    }

    /**
     * Get the protocol charset
     */
    public String getProtocolCharset() {
        return PROTOCOL_CHARSET;
    }
}
