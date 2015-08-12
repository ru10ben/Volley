package com.android.volley.stack;

import com.android.volley.Request;
import com.android.volley.error.AuthFailureError;
import com.android.volley.request.MultiPartRequest;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HTTP;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An HttpStack that performs request over an {@link OkHttpClient}.
 */
public class OkHttpStack implements HttpStack {
    private final OkHttpClient mClient;
    private final UrlRewriter mUrlRewriter;

    public OkHttpStack(OkHttpClient client) {
        this(client, null);
    }

    public OkHttpStack(OkHttpClient client, UrlRewriter urlRewriter) {
        mClient = client;
        mUrlRewriter = urlRewriter;
    }

    @Override
    public HttpResponse performRequest(Request<?> request) throws IOException, AuthFailureError {
        String requestUrl = request.getUrl();
        if (mUrlRewriter != null) {
            String rewritten = mUrlRewriter.rewriteUrl(request);
            if (rewritten == null) {
                throw new IOException("URL blocked by rewriter: " + requestUrl);
            }
            requestUrl = rewritten;
        }

        HashMap<String, String> additionalHeaders = new HashMap<String, String>();
        additionalHeaders.putAll(request.getHeaders());

        OkHttpClient client = mClient.clone();
        int timeoutMs = request.getTimeoutMs();
        client.setConnectTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        client.setReadTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(timeoutMs, TimeUnit.MILLISECONDS);

        com.squareup.okhttp.Request.Builder okHttpRequestBuilder =
                new com.squareup.okhttp.Request.Builder();
        okHttpRequestBuilder.url(requestUrl);

        Map<String, String> headers = request.getHeaders();

        for (final String name : headers.keySet()) {
            okHttpRequestBuilder.addHeader(name, headers.get(name));
        }

        for (final String name : additionalHeaders.keySet()) {
            okHttpRequestBuilder.addHeader(name, additionalHeaders.get(name));
        }

        setConnectionParametersForRequest(okHttpRequestBuilder, request);

        com.squareup.okhttp.Request okHttpRequest = okHttpRequestBuilder.build();
        Call okHttpCall = client.newCall(okHttpRequest);
        Response okHttpResponse = okHttpCall.execute();

        StatusLine responseStatus = new BasicStatusLine
                (
                        parseProtocol(okHttpResponse.protocol()),
                        okHttpResponse.code(),
                        okHttpResponse.message()
                );

        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromOkHttpResponse(okHttpResponse));

        Headers responseHeaders = okHttpResponse.headers();

        for (int i = 0, len = responseHeaders.size(); i < len; i++) {
            final String name = responseHeaders.name(i), value = responseHeaders.value(i);

            if (name != null) {
                response.addHeader(new BasicHeader(name, value));
            }
        }

        return response;
    }

    private static HttpEntity entityFromOkHttpResponse(Response r) throws IOException {
        BasicHttpEntity entity = new BasicHttpEntity();
        ResponseBody body = r.body();

        entity.setContent(body.byteStream());
        entity.setContentLength(body.contentLength());
        entity.setContentEncoding(r.header("Content-Encoding"));

        if (body.contentType() != null) {
            entity.setContentType(body.contentType().type());
        }
        return entity;
    }

    private static void setConnectionParametersForRequest
            (com.squareup.okhttp.Request.Builder builder, Request<?> request)
            throws IOException, AuthFailureError {
        switch (request.getMethod()) {
            case Request.Method.GET:
                builder.get();
                break;

            case Request.Method.DELETE:
                builder.delete();
                break;

            case Request.Method.POST:
                builder.post(createRequestBody(request));
                break;

            case Request.Method.PUT:
                builder.put(createRequestBody(request));
                break;

            case Request.Method.HEAD:
                builder.head();
                break;

            case Request.Method.OPTIONS:
                builder.method("OPTIONS", null);
                break;

            case Request.Method.TRACE:
                builder.method("TRACE", null);
                break;

            case Request.Method.PATCH:
                builder.patch(createRequestBody(request));
                break;

            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

    private static ProtocolVersion parseProtocol(final Protocol p) {
        switch (p) {
            case HTTP_1_0:
                return new ProtocolVersion("HTTP", 1, 0);
            case HTTP_1_1:
                return new ProtocolVersion("HTTP", 1, 1);
            case SPDY_3:
                return new ProtocolVersion("SPDY", 3, 1);
            case HTTP_2:
                return new ProtocolVersion("HTTP", 2, 0);
        }

        throw new IllegalAccessError("Unkwown protocol");
    }

    private static RequestBody createRequestBody(Request request) throws AuthFailureError, IOException {
        if (request instanceof MultiPartRequest) {
            final Map<String, String> multipartParams = request.getParams();
            final Map<String, String> filesToUpload = ((MultiPartRequest) request).getFilesToUpload();
            final MultipartBuilder multipartBuilder = new MultipartBuilder().type(MultipartBuilder.FORM);

            for (Map.Entry<String, String> entry : multipartParams.entrySet()) {
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }

            for (String key : filesToUpload.keySet()) {
                File file = new File(filesToUpload.get(key));

                if(!file.exists()) {
                    throw new IOException(String.format("File not found: %s", file.getAbsolutePath()));
                }

                if(file.isDirectory()) {
                    throw new IOException(String.format("File is a directory: %s", file.getAbsolutePath()));
                }

                multipartBuilder.addFormDataPart(key, file.getName(), RequestBody.create(
                        MediaType.parse(HTTP.DEFAULT_CONTENT_TYPE), file));
            }

            return multipartBuilder.build();
        }

        final byte[] body = request.getBody();
        if (body == null) return null;

        return RequestBody.create(MediaType.parse(request.getBodyContentType()), body);
    }
}
