
package com.android.volley.request;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import com.android.volley.Listener;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

/**
 * A request for making a Multi Part request
 */
public class MultiPartRequest extends Request<String> {

    private static final String         PROTOCOL_CHARSET = "utf-8";

    /**
     * A map for Multipart parameters
     */
    private Map<String, MultiPartParam> mMultipartParams = null;

    /**
     * A map for uploading files
     */
    private Map<String, String>         mFileUploads     = null;
    

    public MultiPartRequest(int method, String url, Listener<String> listener) {
        super(method, url, listener);
        mFileUploads = new HashMap<String, String>();
        mMultipartParams = new HashMap<String, MultiPartRequest.MultiPartParam>();
    }


    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, response.charset);
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, response);
    }

    /**
     * Add a parameter to be sent in the multipart request
     * 
     * @param name
     *            The name of the paramter
     * @param contentType
     *            The content type of the paramter
     * @param value
     *            the value of the paramter
     * @return The Multipart request for chaining calls
     */
    public MultiPartRequest addMultipartParam(String name, String contentType, String value) {
        mMultipartParams.put(name, new MultiPartParam(contentType, value));
        return this;
    }

    /**
     * Add a file to be uploaded in the multipart request
     * 
     * @param name
     *            The name of the file key
     * @param filePath
     *            The path to the file. This file MUST exist.
     * @return The Multipart request for chaining method calls
     */
    public MultiPartRequest addFile(String name, String filePath) {
        mFileUploads.put(name, filePath);
        return this;
    }

    /**
     * A representation of a MultiPart parameter
     */
    public static final class MultiPartParam {

        public String contentType;
        public String value;

        /**
         * Initialize a multipart request param with the value and content type
         * 
         * @param contentType
         *            The content type of the param
         * @param value
         *            The value of the param
         */
        public MultiPartParam(String contentType, String value) {
            this.contentType = contentType;
            this.value = value;
        }
    }

    /**
     * Get all the multipart params for this request
     * 
     * @return A map of all the multipart params NOT including the file uploads
     */
    public Map<String, MultiPartParam> getMultipartParams() {
        return mMultipartParams;
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
