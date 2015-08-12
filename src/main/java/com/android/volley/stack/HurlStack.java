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

package com.android.volley.stack;

import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.error.AuthFailureError;
import com.android.volley.request.MultiPartRequest;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HTTP;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * An {@link HttpStack} based on {@link HttpURLConnection}.
 */
public class HurlStack implements HttpStack {

    private static final String    HEADER_CONTENT_TYPE              = "Content-Type";
    private static final String    HEADER_USER_AGENT                = "User-Agent";
    private static final String    HEADER_CONTENT_DISPOSITION       = "Content-Disposition";
    private static final String    HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final String    CONTENT_TYPE_MULTIPART           = "multipart/form-data; charset=%s; boundary=%s";
    private static final String    BINARY                           = "binary";
    private static final String    CRLF                             = "\r\n";
    private static final String    FORM_DATA                        = "form-data; name=\"%s\"";
    private static final String    BOUNDARY_PREFIX                  = "--";
    private static final String    CONTENT_TYPE_PLAIN_TEXT          = "text/plain";
    private static final String    CONTENT_TYPE_OCTET_STREAM        = "application/octet-stream";
    private static final String    FILENAME                         = "filename=%s";
    private static final String    COLON_SPACE                      = ": ";
    private static final String    SEMICOLON_SPACE                  = "; ";
    
    private final String mUserAgent;
    private final UrlRewriter mUrlRewriter;
    private final SSLSocketFactory mSslSocketFactory;

    public HurlStack(String userAgent) {
        this(userAgent, null);
    }
    
    public HurlStack(String userAgent, UrlRewriter urlRewriter) {
        this(userAgent, urlRewriter, null);
    }
    
    /**
     * @param urlRewriter Rewriter to use for request URLs.
     * @param userAgent to report in your HTTP requests.
     * @param sslSocketFactory SSL factory to use for HTTPS connections.
     */
    public HurlStack(String userAgent, UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
        mUserAgent = userAgent;
        mUrlRewriter = urlRewriter;
        mSslSocketFactory = sslSocketFactory;
    }
    
	 /**
     * Add headers and user agent to an {@code }
     * 
     * @param connection
     *            The {@linkplain HttpURLConnection} to add request headers to
     * @param userAgent
     *            The User Agent to identify on server
     * @param additionalHeaders
     *            The headers to add to the request
     */
    private void addHeadersToConnection(HttpURLConnection connection, String userAgent, 
            Map<String, String> additionalHeaders) {
        connection.setRequestProperty(HEADER_USER_AGENT, userAgent);
        for (String headerName : additionalHeaders.keySet()) {
            connection.addRequestProperty(headerName, additionalHeaders.get(headerName));
        }
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

		HashMap<String, String> map = new HashMap<String, String>();
		map.putAll(request.getHeaders());

        URL parsedUrl = new URL(requestUrl);
        HttpURLConnection connection = openConnection(parsedUrl, request);
	        
		if (request instanceof MultiPartRequest) {
            setConnectionParametersForMultipartRequest(connection, request, map);
        } else {
            setConnectionParametersForRequest(connection, request, map);
        }

		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			// -1 is returned by getResponseCode() if the response code could not be retrieved.
			// Signal to the caller that something was wrong with the connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}

		StatusLine responseStatus = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1),
                connection.getResponseCode(), connection.getResponseMessage());
        BasicHttpResponse response = new BasicHttpResponse(responseStatus);
        response.setEntity(entityFromConnection(connection));

        for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            if (header.getKey() != null) {
                Header h = new BasicHeader(header.getKey(), convertHeaderFileds(header.getValue()));
                response.addHeader(h);
            }
        }

        return response;
    }

    /**
     * The same key with append value. The 'Set-Cookie' may hava more than one.
     */
    private String convertHeaderFileds(List<String> cookies) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> iterator = cookies.iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next());
            sb.append(";");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length()-1);
        }
        return sb.toString();
    }
    
    /**
     * Initializes an {@link HttpEntity} from the given {@link HttpURLConnection}.
     * @return an HttpEntity populated with data from <code>connection</code>.
     */
    private HttpEntity entityFromConnection(HttpURLConnection connection) {
        BasicHttpEntity entity = new BasicHttpEntity();
        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException ioe) {
            inputStream = connection.getErrorStream();
        }
        entity.setContent(inputStream);
        entity.setContentLength(connection.getContentLength());
        entity.setContentEncoding(connection.getContentEncoding());
        entity.setContentType(connection.getContentType());
        return entity;
    }

    /**
     * Create an {@link HttpURLConnection} for the specified {@code url}.
     */
    private HttpURLConnection createConnection(URL url) throws IOException {
        trustAllHttpsCertificates();
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Opens an {@link HttpURLConnection} with parameters.
     * @return an open connection
     */
    private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
        HttpURLConnection connection = createConnection(url);

        int timeoutMs = request.getTimeoutMs();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);
        connection.setUseCaches(false);
        connection.setDoInput(true);

        // use caller-provided custom SslSocketFactory, if any, for HTTPS
        if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
            ((HttpsURLConnection)connection).setSSLSocketFactory(mSslSocketFactory);
        }

        return connection;
    }

	private void setConnectionParametersForRequest(HttpURLConnection connection, Request<?> request, 
	        HashMap<String, String> additionalHeaders) throws IOException, AuthFailureError {
	    
	    addHeadersToConnection(connection, mUserAgent, additionalHeaders);
        switch (request.getMethod()) {
            case Method.GET:
                // Not necessary to set the request method because connection defaults to GET but
                // being explicit here.
                connection.setRequestMethod("GET");
                break;
            case Method.DELETE:
                connection.setRequestMethod("DELETE");
                break;
            case Method.POST:
                connection.setRequestMethod("POST");
                addBodyIfExists(connection, request);
                break;
            case Method.PUT:
                connection.setRequestMethod("PUT");
                addBodyIfExists(connection, request);
                break;
            case Method.HEAD:
                connection.setRequestMethod("HEAD");
                break;
            case Method.OPTIONS:
                connection.setRequestMethod("OPTIONS");
                break;
            case Method.TRACE:
                connection.setRequestMethod("TRACE");
                break;
            case Method.PATCH:
                addBodyIfExists(connection, request);
                connection.setRequestMethod("PATCH");
                break;
            default:
                throw new IllegalStateException("Unknown method type.");
        }
    }

	 /**
     * Perform a multipart request on a connection
     * 
     * @param connection The Connection to perform the multi part request
     * @param request
     * @param additionalHeaders
     */
    private void setConnectionParametersForMultipartRequest(HttpURLConnection connection, Request<?> request, 
            HashMap<String, String> additionalHeaders) throws IOException, AuthFailureError {
        final String charset = ((MultiPartRequest) request).getProtocolCharset();
        final int curTime = (int) (System.currentTimeMillis() / 1000);
        final String boundary = BOUNDARY_PREFIX + curTime;
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty(HEADER_CONTENT_TYPE, String.format(CONTENT_TYPE_MULTIPART, charset, curTime));
        connection.setChunkedStreamingMode(0);

        Map<String, String> multipartParams = request.getParams();
        Map<String, String> filesToUpload = ((MultiPartRequest) request).getFilesToUpload();
        PrintWriter writer = null;
        try {
            addHeadersToConnection(connection, mUserAgent, additionalHeaders);
            OutputStream out = connection.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(out, charset), true);

            for (Entry<String, String> entry : multipartParams.entrySet()) {
                writer.append(boundary)
                        .append(CRLF)
                        .append(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA, entry.getKey()))
                        .append(CRLF)
                        .append(HEADER_CONTENT_TYPE + COLON_SPACE + CONTENT_TYPE_PLAIN_TEXT)
                        .append(CRLF)
                        .append(CRLF)
                        .append(entry.getValue())
                        .append(CRLF)
                        .flush();
            }

            for (String key : filesToUpload.keySet()) {
                File file = new File(filesToUpload.get(key));
                
                if(!file.exists()) {
                    throw new IOException(String.format("File not found: %s", file.getAbsolutePath()));
                }
                
                if(file.isDirectory()) {
                    throw new IOException(String.format("File is a directory: %s", file.getAbsolutePath()));
                }

                writer.append(boundary)
                        .append(CRLF)
                        .append(String.format(HEADER_CONTENT_DISPOSITION
                                + COLON_SPACE + FORM_DATA + SEMICOLON_SPACE
                                + FILENAME, key, file.getName()))
                        .append(CRLF)
                        .append(HEADER_CONTENT_TYPE + COLON_SPACE + CONTENT_TYPE_OCTET_STREAM)
                        .append(CRLF)
                        .append(HEADER_CONTENT_TRANSFER_ENCODING + COLON_SPACE + BINARY)
                        .append(CRLF)
                        .append(CRLF)
                        .flush();

                BufferedInputStream input = null;
                try {
                    FileInputStream fis = new FileInputStream(file);
                    input = new BufferedInputStream(fis);
                    int bufferLength = 0;

                    byte[] buffer = new byte[4*1024];
                    while ((bufferLength = input.read(buffer)) > 0) {
                        out.write(buffer, 0, bufferLength);
                    }
                    out.flush(); // Important! Output cannot be closed. Close of writer will close output as well.
                } finally {
                    if (input != null)
                        try {
                            input.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                }
                writer.append(CRLF).flush(); // CRLF is important! It indicates end of binary boundary.
            }

            // End of multipart/form-data.
            writer.append(boundary + BOUNDARY_PREFIX).append(CRLF).flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }
	
    private void addBodyIfExists(HttpURLConnection connection, Request<?> request) 
            throws IOException, AuthFailureError {
        byte[] body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty(HTTP.CONTENT_TYPE, request.getBodyContentType());
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());
            out.write(body);
            out.close();
        }
    }
    
    /**
     * 如果请求是https请求那么就信任所有SSL
     */
    private void trustAllHttpsCertificates() {
        try {
            javax.net.ssl.X509TrustManager tm = new javax.net.ssl.X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {return null;}
                @Override
                public void checkServerTrusted(X509Certificate[] chain,String authType) throws CertificateException {}
                @Override
                public void checkClientTrusted(X509Certificate[] chain,String authType) throws CertificateException {}
            };
            // Create a trust manager that does not validate certificate chains:
            javax.net.ssl.TrustManager[] trustAllCerts = { tm };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS"); 
            sc.init(null, trustAllCerts, null);
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            javax.net.ssl.HostnameVerifier hv = new javax.net.ssl.HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {return true;}
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
