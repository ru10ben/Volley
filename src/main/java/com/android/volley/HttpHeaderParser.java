package com.android.volley;

import android.text.TextUtils;
import com.android.volley.error.ServerError;
import com.android.volley.net.HTTP;
import com.android.volley.net.HttpResponse;
import com.android.volley.net.ResponseData;
import com.android.volley.toolbox.ByteArrayPool;
import com.android.volley.toolbox.PoolingByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class HttpHeaderParser {

    /**
     * Reads the contents of HttpEntity into a byte[].
     */
    public static byte[] responseToBytes(Request<?> request, HttpResponse response, ResponseDelivery delivery)
            throws IOException, ServerError {
        ResponseData entity = response.getBody();
        PoolingByteArrayOutputStream bytes =
                new PoolingByteArrayOutputStream(ByteArrayPool.get(), (int) entity.length());
        byte[] buffer = null;
        long totalSize = entity.length();
        try {
            InputStream in = entity.stream();
            if (isGzipContent(response) && !(in instanceof GZIPInputStream)) {
                in = new GZIPInputStream(in);
            }

            if (in == null) {
                throw new ServerError();
            }

            buffer = ByteArrayPool.get().getBuf(1024);
            int count;
            int transferredBytes = 0;
            while ((count = in.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
                transferredBytes += count;
                delivery.postProgress(request, totalSize, transferredBytes);
            }
            return bytes.toByteArray();
        } finally {
            try {
                // Close the InputStream and release the resources by "consuming the content".
                entity.close();
            } catch (IOException e) {
                // This can happen if there was an exception above that left the entity in
                // an invalid state.
                VolleyLog.v("Error occured when calling consumingContent");
            }
            ByteArrayPool.get().returnBuf(buffer);
            bytes.close();
        }
    }

    /**
     * Returns the charset specified in the Content-Type of this header.
     */
    public static String getCharset(HttpResponse response) {
        String contentType = response.getHeader(HTTP.CONTENT_TYPE);
        if (!TextUtils.isEmpty(contentType)) {
            String[] params = contentType.split(";");
            for (int i = 1; i < params.length; i++) {
                String[] pair = params[i].trim().split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("charset")) {
                        return pair[1];
                    }
                }
            }
        }
        return null;
    }

    public static String getHeader(HttpResponse response, String key) {
        return response.getHeader(key);
    }

    public static boolean isSupportRange(HttpResponse response) {
        if (TextUtils.equals(getHeader(response, "Accept-Ranges"), "bytes")) {
            return true;
        }
        String value = getHeader(response, HTTP.CONTENT_RANGE);
        return value != null && value.startsWith("bytes");
    }

    public static boolean isGzipContent(HttpResponse response) {
        return TextUtils.equals(getHeader(response, HTTP.CONTENT_ENCODING), "gzip");
    }
}
