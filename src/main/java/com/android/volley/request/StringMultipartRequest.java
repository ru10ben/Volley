package com.android.volley.request;

import com.android.volley.Listener;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import java.io.UnsupportedEncodingException;

public class StringMultipartRequest extends MultiPartRequest<String> {

    public StringMultipartRequest(int method, String url, Listener<String> listener) {
        super(method, url, listener);
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
}
