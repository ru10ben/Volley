package com.android.volley.net.impl;

import com.android.volley.net.ContentType;
import com.android.volley.net.HTTP;
import com.android.volley.net.HttpResponse;
import com.android.volley.net.ResponseData;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okio.BufferedSink;
import okio.Okio;

/**
 * Name: OkHttpResponse
 * User: Lee (darkeet.me@gmail.com)
 * Date: 2015/10/29 18:37
 * Desc:
 */
public class OkHttpResponse extends HttpResponse {
    private ResponseData body;
    private final Response response;

    public OkHttpResponse(Response response) {
        this.response = response;
    }

    @Override
    public int getStatusCode() {
        return response.code();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        final Headers headers = response.headers();
        final Map<String, List<String>> headersList = new HashMap<>();
        for (int i = 0, j = headers.size(); i < j; i++) {
            if (headersList.containsKey(headers.name(i))) {
                headersList.get(headers.name(i)).add(headers.value(i));
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(headers.value(i));
                headersList.put(headers.name(i), list);
            }
        }
        return headersList;
    }

    @Override
    public String getHeader(String name) {
        return response.header(name);
    }

    @Override
    public String[] getHeaders(String name) {
        final List<String> values = response.headers(name);
        return values.toArray(new String[values.size()]);
    }

    @Override
    public ResponseData getBody() {
        if (body != null) return body;
        return body = new OkHttpToBody(response.body());
    }

    @Override
    public void close() throws IOException {
        if (body != null) {
            body.close();
            body = null;
        }
    }

    private class OkHttpToBody implements ResponseData {
        private ResponseBody body;

        public OkHttpToBody(ResponseBody body) {
            this.body = body;
        }

        @Override
        public ContentType contentType() {
            final MediaType mediaType = body.contentType();
            if (mediaType == null) return null;
            return ContentType.parse(mediaType.toString());
        }

        @Override
        public String contentEncoding() {
            return getHeader(HTTP.CONTENT_ENCODING);
        }

        @Override
        public long length() throws IOException {
            return body.contentLength();
        }

        @Override
        public void writeTo(OutputStream os) throws IOException {
            final BufferedSink sink = Okio.buffer(Okio.sink(os));
            sink.writeAll(body.source());
            sink.flush();
        }

        @Override
        public InputStream stream() throws IOException {
            return body.byteStream();
        }

        @Override
        public void close() throws IOException {
            body.close();
        }
    }
}
