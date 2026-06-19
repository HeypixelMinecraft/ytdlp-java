package com.ytdlp.networking;

import okhttp3.ResponseBody;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP response wrapper around OkHttp {@link okhttp3.Response}.
 */
public class Response implements AutoCloseable {
    private final okhttp3.Response okResponse;

    public Response(okhttp3.Response okResponse) {
        this.okResponse = okResponse;
    }

    public int getStatusCode() {
        return okResponse.code();
    }

    public Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        for (String name : okResponse.headers().names()) {
            headers.put(name, okResponse.header(name));
        }
        return headers;
    }

    public String getHeader(String name) {
        return okResponse.header(name);
    }

    public InputStream getBody() {
        ResponseBody body = okResponse.body();
        if (body == null) {
            return InputStream.nullInputStream();
        }
        return body.byteStream();
    }

    public String getUrl() {
        return okResponse.request().url().toString();
    }

    public okhttp3.Response getOkResponse() {
        return okResponse;
    }

    @Override
    public void close() {
        okResponse.close();
    }
}
