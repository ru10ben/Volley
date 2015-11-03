package com.android.volley.net;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Name: HttpResponse
 * User: Lee (darkeet.me@gmail.com)
 * Date: 2015/10/29 15:11
 * Desc:
 */
public abstract class HttpResponse implements Closeable {

    public abstract int getStatusCode();

    public abstract ResponseData getBody();

    public abstract Map<String, List<String>> getHeaders();


    public String getHeader(String name) {
        if (name == null) throw new NullPointerException();
        final Map<String, List<String>> headers = getHeaders();
        if (headers == null) return null;
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (name.equalsIgnoreCase(header.getKey())) {
                return header.getValue().get(0);
            }
        }
        return null;
    }

    public String[] getHeaders(String name) {
        if (name == null) throw new NullPointerException();
        final Map<String, List<String>> headers = getHeaders();
        if (headers == null) return new String[0];

        final ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (name.equalsIgnoreCase(header.getKey())) {
                result.addAll(header.getValue());
            }
        }
        return result.toArray(new String[result.size()]);
    }
}
