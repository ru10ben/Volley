package com.android.volley.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Name: ResponseData
 * User: Lee (darkeet.me@gmail.com)
 * Date: 2015/10/29 15:11
 * Desc:
 */
public interface ResponseData extends Closeable {

    ContentType contentType();

    String contentEncoding();

    long length() throws IOException;

    void writeTo(OutputStream os) throws IOException;

    InputStream stream() throws IOException;

    void close() throws IOException;
}
