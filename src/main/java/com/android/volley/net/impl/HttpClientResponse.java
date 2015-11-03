package com.android.volley.net.impl;

import com.android.volley.net.ContentType;
import com.android.volley.net.HttpResponse;
import com.android.volley.net.ResponseData;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Name: HttpClientResponse
 * User: Lee (darkeet.me@gmail.com)
 * Date: 2015/10/29 18:38
 * Desc:
 */
public class HttpClientResponse extends HttpResponse {
    private org.apache.http.HttpResponse httpResponse;
    private ResponseData body;

    public HttpClientResponse(org.apache.http.HttpResponse response) {
        this.httpResponse = response;
    }

    @Override
    public int getStatusCode() {
        return httpResponse.getStatusLine().getStatusCode();
    }

    @Override
    public ResponseData getBody() {
        if (body != null) return body;
        return (body = new HttpClientToBody(httpResponse.getEntity()));
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        final Header[] headers = httpResponse.getAllHeaders();
        final HashMap<String, List<String>> headersList = new HashMap<>();
        for (Header header : headers) {
            if (headersList.containsKey(header.getName())) {
                headersList.get(header.getName()).add(header.getValue());
            } else {
                ArrayList<String> list = new ArrayList<>();
                list.add(header.getValue());
                headersList.put(header.getName(), list);
            }
        }
        return headersList;
    }

    @Override
    public void close() throws IOException {
        if (body != null) {
            body.close();
            body = null;
        }
    }


    private class HttpClientToBody implements ResponseData {
        private HttpEntity httpEntity;

        public HttpClientToBody(HttpEntity httpEntity) {
            this.httpEntity = httpEntity;
        }

        @Override
        public ContentType contentType() {
            Header header = httpEntity.getContentType();
            return (header != null ? ContentType.parse(header.getValue()) : null);
        }

        @Override
        public String contentEncoding() {
            Header header = httpEntity.getContentEncoding();
            return (header != null ? header.getValue() : null);
        }

        @Override
        public long length() throws IOException {
            return httpEntity.getContentLength();
        }

        @Override
        public void writeTo(OutputStream os) throws IOException {
            final InputStream instream = stream();
            try {
                int l;
                final byte[] tmp = new byte[4096];
                while ((l = instream.read(tmp)) != -1) {
                    os.write(tmp, 0, l);
                }
            } finally {
                instream.close();
            }
        }

        @Override
        public InputStream stream() throws IOException {
            return httpEntity.getContent();
        }

        @Override
        public void close() throws IOException {
            httpEntity.consumeContent();
        }
    }
}
