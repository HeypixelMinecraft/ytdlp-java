package com.ytdlp.util;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeUrlUtils {
    private static final Pattern LIST_PARAM = Pattern.compile("[?&]list=([a-zA-Z0-9_-]+)");
    private static final Pattern VIDEO_PARAM = Pattern.compile("[?&]v=([0-9A-Za-z_-]{11})");
    private static final Pattern PLAYLIST_PATH = Pattern.compile("(?i)youtube\\.com/playlist\\?");

    private YoutubeUrlUtils() {
    }

    public static boolean isPlaylistUrl(String url) {
        return PLAYLIST_PATH.matcher(url).find() || extractPlaylistId(url) != null;
    }

    public static String extractPlaylistId(String url) {
        Matcher m = LIST_PARAM.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static String extractVideoId(String url) {
        Matcher m = VIDEO_PARAM.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        Matcher shortPath = Pattern.compile("(?i)youtu\\.be/([0-9A-Za-z_-]{11})").matcher(url);
        if (shortPath.find()) {
            return shortPath.group(1);
        }
        return null;
    }

    public static String buildWatchUrl(String videoId) {
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    public static String buildPlaylistUrl(String playlistId) {
        return "https://www.youtube.com/playlist?list=" + playlistId;
    }

    public static Map<String, String> parseQuery(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        try {
            URI uri = URI.create(url);
            String query = uri.getQuery();
            if (query == null) {
                return params;
            }
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    params.put(pair.substring(0, eq), pair.substring(eq + 1));
                }
            }
        } catch (Exception ignored) {
        }
        return params;
    }
}
