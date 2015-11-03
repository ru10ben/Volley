/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.toolbox;

import android.os.SystemClock;
import com.android.volley.*;
import com.android.volley.error.AuthFailureError;
import com.android.volley.error.NetworkError;
import com.android.volley.error.NoConnectionError;
import com.android.volley.error.RedirectError;
import com.android.volley.error.ServerError;
import com.android.volley.error.TimeoutError;
import com.android.volley.error.VolleyError;
import com.android.volley.net.HttpResponse;
import com.android.volley.net.HttpStatus;
import com.android.volley.net.ResponseData;
import com.android.volley.stack.HttpStack;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A network performing Netroid requests over an {@link HttpStack}.
 */
public class BasicNetwork implements Network {
    protected static final boolean DEBUG = VolleyLog.DEBUG;

    private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

    private static int DEFAULT_POOL_SIZE = 4096;

    private final HttpStack mHttpStack;

    /**
     * The default charset only use when response doesn't offer the Content-Type header.
     */
    private final String mDefaultCharset;

    /**
     * Request delivery mechanism.
     */
    private ResponseDelivery mDelivery;

    /**
     * Constructor
     *
     * @param httpStack HTTP stack to be used
     */
    public BasicNetwork(HttpStack httpStack) {
        this(httpStack, "UTF-8");
    }

    /**
     * Constructor
     *
     * @param httpStack      HTTP stack to be used
     * @param defaultCharset default charset if response does not provided.
     */
    public BasicNetwork(HttpStack httpStack, String defaultCharset) {
        // If a pool isn't passed in, then build a small default pool that will give us a lot of
        // benefit and not use too much memory.
        this(httpStack, DEFAULT_POOL_SIZE, defaultCharset);
    }

    /**
     * Constructor
     *
     * @param httpStack      HTTP stack to be used
     * @param bytePoolSize   Size of buffer pool that improves GC performance in copy operations.
     * @param defaultCharset when Http Header doesn't offer the 'Content-Type:Charset', it will be use.
     */
    public BasicNetwork(HttpStack httpStack, int bytePoolSize, String defaultCharset) {
        ByteArrayPool.init(bytePoolSize);
        mDefaultCharset = defaultCharset;
        mHttpStack = httpStack;
    }

    @Override
    public void setDelivery(ResponseDelivery delivery) {
        mDelivery = delivery;
    }

    @Override
    public NetworkResponse performRequest(Request<?> request) throws VolleyError {
        // Determine if request had non-http perform.
        NetworkResponse networkResponse = request.perform();
        if (networkResponse != null) return networkResponse;

        long requestStart = SystemClock.elapsedRealtime();
        while (true) {
            byte[] responseContents = null;
            HttpResponse httpResponse = null;
            Map<String, String> responseHeaders = null;

            try {
                // prepare to perform this request, normally is reset the request headers.
                request.prepare();

                httpResponse = mHttpStack.performRequest(request);

                ResponseData body = httpResponse.getBody();
                VolleyLog.d("Http response length=%d，contentType=%s，contentEncoding=%s",
                        body.length(), body.contentType(), body.contentEncoding());

                int statusCode = httpResponse.getStatusCode();
                responseHeaders = convertHeaders(httpResponse.getHeaders());
                // Handle moved resources
                if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY || statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                    String newUrl = responseHeaders.get("Location");
                    request.setRedirectUrl(newUrl);
                }

                // Handle the response for various request
                responseContents = request.handleResponse(httpResponse, mDelivery);

                // if the request is slow, log it.
                long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
                logSlowRequests(requestLifetime, request, responseContents, httpResponse);

                if (statusCode < 200 || statusCode > 299) {
                    throw new IOException();
                }

                return new NetworkResponse(statusCode, responseContents, responseHeaders, parseCharset(httpResponse));
            } catch (SocketTimeoutException e) {
                attemptRetryOnException("socket", request, new TimeoutError());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Bad URL " + request.getUrl(), e);
            } catch (IOException e) {
                if (httpResponse == null)
                    throw new NoConnectionError(e);

                int statusCode = httpResponse.getStatusCode();
                if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                        statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                    VolleyLog.e("Request at %s has been redirected to %s", request.getOriginUrl(), request.getUrl());
                } else {
                    VolleyLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());
                }
                if (responseContents != null) {
                    networkResponse = new NetworkResponse(statusCode, responseContents,
                            responseHeaders, parseCharset(httpResponse));
                    if (statusCode == HttpStatus.SC_UNAUTHORIZED ||
                            statusCode == HttpStatus.SC_FORBIDDEN) {
                        attemptRetryOnException("auth",
                                request, new AuthFailureError(networkResponse));
                    } else if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
                            statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                        attemptRetryOnException("redirect",
                                request, new RedirectError(networkResponse));
                    } else {
                        throw new ServerError(networkResponse);
                    }
                } else {
                    throw new NetworkError(e);
                }
            }
        }
    }

    /**
     * Logs requests that took over SLOW_REQUEST_THRESHOLD_MS to complete.
     */
    private void logSlowRequests(long requestLifetime, Request<?> request,
                                 byte[] responseContents, HttpResponse response) {
        if (DEBUG || requestLifetime > SLOW_REQUEST_THRESHOLD_MS) {
            VolleyLog.d("HTTP response for request=<%s> [lifetime=%d], [size=%s], " +
                            "[rc=%d], [retryCount=%s]", request, requestLifetime,
                    responseContents != null ? responseContents.length : "null",
                    response.getStatusCode(), request.getRetryPolicy().getCurrentRetryCount());
        }
    }

    /**
     * Attempts to prepare the request for a retry. If there are no more attempts remaining in the
     * request's retry policy, a timeout exception is thrown.
     *
     * @param request The request to use.
     */
    private void attemptRetryOnException(String logPrefix, Request<?> request,
                                         VolleyError exception) throws VolleyError {
        RetryPolicy retryPolicy = request.getRetryPolicy();
        int oldTimeout = request.getTimeoutMs();

        try {
            retryPolicy.retry(exception);
        } catch (VolleyError e) {
            request.addMarker(String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
            throw e;
        }

        request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
        mDelivery.postRetry(request);
    }

    protected void logError(String what, String url, long start) {
        long now = SystemClock.elapsedRealtime();
        VolleyLog.v("HTTP ERROR(%s) %d ms to fetch %s", what, (now - start), url);
    }

    private Map<String, String> convertHeaders(Map<String, List<String>> headers) {
        Map<String, String> result = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            result.put(header.getKey(), join(header.getValue(), ";"));
        }
        return result;
    }

    private String join(List<String> list, String replacement) {
        if (list.size() == 1) return list.get(0);
        StringBuilder b = new StringBuilder();
        for (String item : list) {
            b.append(replacement).append(item);
        }
        return b.toString().substring(replacement.length());
    }

    /**
     * Returns the charset specified in the Content-Type of this header,
     * or the defaultCharset if none can be found.
     */
    private String parseCharset(HttpResponse response) {
        String charset = HttpHeaderParser.getCharset(response);
        return charset == null ? mDefaultCharset : charset;
    }
}
