package com.ytdlp.util;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlUtils {
    private static final Pattern EXT_PATTERN = Pattern.compile("\\.([a-zA-Z0-9]{2,5})(?:[?#]|$)");

    private UrlUtils() {
    }

    public static String determineExt(String url, String defaultExt) {
        if (url == null) {
            return defaultExt;
        }
        Matcher matcher = EXT_PATTERN.matcher(url);
        if (matcher.find()) {
            String ext = matcher.group(1).toLowerCase();
            if (!ext.equals("php") && !ext.equals("html")) {
                return ext;
            }
        }
        return defaultExt;
    }

    public static String filenameFromUrl(String url) {
        try {
            String path = URI.create(url).getPath();
            int slash = path.lastIndexOf('/');
            return slash >= 0 ? path.substring(slash + 1) : path;
        } catch (Exception e) {
            return "video";
        }
    }

    public static String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "video";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    public static String expandOutputTemplate(String template, String id, String title, String ext) {
        String result = template;
        result = result.replace("%(id)s", id != null ? id : "");
        result = result.replace("%(title)s", title != null ? sanitizeFilename(title) : "");
        result = result.replace("%(ext)s", ext != null ? ext : "mp4");
        return result;
    }

    public static String mimeToExt(String mimeType) {
        if (mimeType == null) {
            return "mp4";
        }
        if (mimeType.contains("webm")) {
            return "webm";
        }
        if (mimeType.contains("mp4") || mimeType.contains("mpeg4")) {
            return "mp4";
        }
        if (mimeType.contains("mp3") || mimeType.contains("mpeg")) {
            return "mp3";
        }
        if (mimeType.contains("m4a")) {
            return "m4a";
        }
        return "mp4";
    }
}
