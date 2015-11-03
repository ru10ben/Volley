package com.android.volley.net.impl;

import com.android.volley.net.ContentType;
import com.android.volley.net.HttpResponse;
import com.android.volley.net.ResponseData;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * Name: HttpUrlConnectionResponse
 * User: Lee (darkeet.me@gmail.com)
 * Date: 2015/10/29 18:35
 * Desc:
 */
public class HttpUrlConnectionResponse extends HttpResponse {
    private HttpURLConnection urlConnection;
    private ResponseData body;

    public HttpUrlConnectionResponse(HttpURLConnection connection) {
        this.urlConnection = connection;
    }

    @Override
    public int getStatusCode() {
        try {
            return urlConnection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return urlConnection.getHeaderFields();
    }

    @Override
    public ResponseData getBody() {
        if (body != null) return body;
        return body = new HttpUrlConnectionToBody(urlConnection);
    }

    @Override
    public void close() throws IOException {
        if (body != null) {
            body.close();
            body = null;
        }
    }


    private class HttpUrlConnectionToBody implements ResponseData {
        private HttpURLConnection connection;

        public HttpUrlConnectionToBody(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
        public ContentType contentType() {
            return ContentType.parse(connection.getContentType());
        }

        @Override
        public String contentEncoding() {
            return connection.getContentEncoding();
        }

        @Override
        public long length() throws IOException {
            return connection.getContentLength();
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
            return connection.getInputStream();
        }

        @Override
        public void close() throws IOException {
            connection.disconnect();
        }
    }
}
