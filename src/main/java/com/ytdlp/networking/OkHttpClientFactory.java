package com.ytdlp.networking;

import com.ytdlp.YoutubeDLOptions;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Builds a configured {@link OkHttpClient} from {@link YoutubeDLOptions}.
 */
public final class OkHttpClientFactory {

    private OkHttpClientFactory() {
    }

    public static OkHttpClient create(YoutubeDLOptions options) {
        return create(options, null);
    }

    public static OkHttpClient create(YoutubeDLOptions options, OkHttpClient baseClient) {
        OkHttpClient.Builder builder = baseClient != null
                ? baseClient.newBuilder()
                : new OkHttpClient.Builder();

        builder.connectTimeout(options.getSocketTimeout(), TimeUnit.SECONDS)
                .readTimeout(options.getSocketTimeout(), TimeUnit.SECONDS)
                .writeTimeout(options.getSocketTimeout(), TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .cookieJar(loadCookieJar(options));

        if (options.getProxy() != null && !options.getProxy().isBlank()) {
            URI proxyUri = URI.create(options.getProxy());
            int port = proxyUri.getPort() > 0 ? proxyUri.getPort() : 8080;
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUri.getHost(), port)));
        }

        return builder.build();
    }

    private static CookieJar loadCookieJar(YoutubeDLOptions options) {
        if (options.getCookieFile() == null || options.getCookieFile().isBlank()) {
            return CookieJar.NO_COOKIES;
        }
        List<Cookie> cookies = loadNetscapeCookies(options.getCookieFile());
        return new CookieJar() {
            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> responseCookies) {
            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> matched = new ArrayList<>();
                for (Cookie cookie : cookies) {
                    if (cookie.matches(url)) {
                        matched.add(cookie);
                    }
                }
                return matched;
            }
        };
    }

    private static List<Cookie> loadNetscapeCookies(String cookieFile) {
        try {
            List<String> lines = Files.readAllLines(Path.of(cookieFile));
            List<Cookie> cookies = new ArrayList<>();
            for (String line : lines) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t");
                if (parts.length < 7) {
                    continue;
                }
                boolean secure = "TRUE".equalsIgnoreCase(parts[3]);
                Cookie.Builder cookieBuilder = new Cookie.Builder()
                        .domain(parts[0])
                        .path(parts[2])
                        .name(parts[5])
                        .value(parts[6]);
                if (secure) {
                    cookieBuilder.secure();
                }
                cookies.add(cookieBuilder.build());
            }
            return cookies;
        } catch (IOException e) {
            throw new com.ytdlp.exception.YtDlpException("Failed to load cookies from " + cookieFile, e);
        }
    }
}
