package com.ytdlp.networking;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private final String url;
    private final String method;
    private final Map<String, String> headers;
    private final byte[] body;

    public Request(String url) {
        this(url, "GET", null, null);
    }

    public Request(String url, String method, byte[] body, Map<String, String> headers) {
        this.url = url;
        this.method = method != null ? method : "GET";
        this.body = body;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public Request withHeader(String name, String value) {
        Map<String, String> newHeaders = new HashMap<>(headers);
        newHeaders.put(name, value);
        return new Request(url, method, body, newHeaders);
    }
}
