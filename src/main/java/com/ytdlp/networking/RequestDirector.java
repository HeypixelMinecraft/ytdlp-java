package com.ytdlp.networking;

import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.exception.YtDlpException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * HTTP request director backed by OkHttp3.
 * Ported from yt-dlp's RequestDirector / networking layer.
 */
public class RequestDirector implements AutoCloseable {
    private final YoutubeDLOptions options;
    private final OkHttpClient client;
    private final boolean ownsClient;

    public RequestDirector(YoutubeDLOptions options) {
        this(options, null);
    }

    public RequestDirector(YoutubeDLOptions options, OkHttpClient client) {
        this.options = options;
        if (client != null) {
            this.client = client;
            this.ownsClient = false;
        } else {
            this.client = OkHttpClientFactory.create(options);
            this.ownsClient = true;
        }
    }

    public OkHttpClient getClient() {
        return client;
    }

    public Response send(Request request) {
        try {
            okhttp3.Response okResponse = client.newCall(toOkHttpRequest(request)).execute();
            return new Response(okResponse);
        } catch (IOException e) {
            throw new YtDlpException("HTTP request failed: " + request.getUrl(), e);
        }
    }

    public String downloadString(Request request) {
        try (Response response = send(request)) {
            if (response.getStatusCode() >= 400) {
                throw new YtDlpException("HTTP " + response.getStatusCode() + " for " + request.getUrl());
            }
            okhttp3.ResponseBody body = response.getOkResponse().body();
            return body != null ? body.string() : "";
        } catch (IOException e) {
            throw new YtDlpException("Failed to download " + request.getUrl(), e);
        }
    }

    public byte[] downloadBytes(Request request) {
        try (Response response = send(request)) {
            if (response.getStatusCode() >= 400) {
                throw new YtDlpException("HTTP " + response.getStatusCode() + " for " + request.getUrl());
            }
            okhttp3.ResponseBody body = response.getOkResponse().body();
            return body != null ? body.bytes() : new byte[0];
        } catch (IOException e) {
            throw new YtDlpException("Failed to download " + request.getUrl(), e);
        }
    }

    private okhttp3.Request toOkHttpRequest(Request request) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url(request.getUrl());

        String userAgent = options.getUserAgent();
        if (userAgent != null && !userAgent.isBlank()) {
            builder.header("User-Agent", userAgent);
        }

        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        String method = request.getMethod().toUpperCase(Locale.ROOT);
        switch (method) {
            case "POST" -> {
                byte[] body = request.getBody() != null ? request.getBody() : new byte[0];
                String contentType = request.getHeaders().getOrDefault("Content-Type", "application/json");
                builder.post(RequestBody.create(body, MediaType.parse(contentType)));
            }
            case "HEAD" -> builder.head();
            case "GET" -> builder.get();
            default -> builder.method(method, request.getBody() != null
                    ? RequestBody.create(request.getBody(), MediaType.parse("application/octet-stream"))
                    : null);
        }

        return builder.build();
    }

    @Override
    public void close() {
        if (ownsClient) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}
