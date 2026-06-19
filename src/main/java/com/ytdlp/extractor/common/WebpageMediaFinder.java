package com.ytdlp.extractor.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.JsonUtils;
import com.ytdlp.util.UrlUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers direct media URLs from HTML pages using OpenGraph, HTML5 video,
 * JSON-LD, inline JSON blobs, and regex scanning — the core strategy behind
 * most yt-dlp generic/simple extractors, implemented in Java.
 */
public final class WebpageMediaFinder {
    private static final Pattern M3U8_PATTERN = Pattern.compile(
            "(?i)https?://[^\"'\\s<>]+\\.m3u8(?:[^\"'\\s<>]*)?");
    private static final Pattern MPD_PATTERN = Pattern.compile(
            "(?i)https?://[^\"'\\s<>]+\\.mpd(?:[^\"'\\s<>]*)?");
    private static final Pattern MP4_PATTERN = Pattern.compile(
            "(?i)https?://[^\"'\\s<>]+\\.mp4(?:[^\"'\\s<>]*)?");
    private static final Pattern WEBM_PATTERN = Pattern.compile(
            "(?i)https?://[^\"'\\s<>]+\\.webm(?:[^\"'\\s<>]*)?");
    private static final Pattern JSON_BLOB_PATTERN = Pattern.compile(
            "(?is)<script[^>]*>(?:\\s*window\\.)?(?:__INITIAL_STATE__|__NEXT_DATA__|__NUXT__|"
                    + "ytInitialPlayerResponse|flashvars|playerConfig|videoInfo)\\s*=\\s*(\\{.+?\\})\\s*;?\\s*</script>");

    private WebpageMediaFinder() {
    }

    public static PageMetadata parseMetadata(String html) {
        Document doc = Jsoup.parse(html);
        String title = metaContent(doc, "og:title");
        if (title == null) {
            title = metaContent(doc, "twitter:title");
        }
        if (title == null) {
            title = doc.title();
        }
        String description = metaContent(doc, "og:description");
        if (description == null) {
            description = metaContent(doc, "description");
        }
        String thumbnail = metaContent(doc, "og:image");
        if (thumbnail == null) {
            thumbnail = metaContent(doc, "twitter:image");
        }
        return new PageMetadata(title, description, thumbnail);
    }

    public static List<Format> findFormats(String html, String pageUrl, List<String> extraRegexes) {
        Set<String> seen = new LinkedHashSet<>();
        List<Format> formats = new ArrayList<>();
        int formatId = 0;

        Document doc = Jsoup.parse(html, pageUrl);

        for (String url : collectOpenGraphVideos(doc)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : collectHtml5Sources(doc)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : collectJsonLdVideos(doc)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : collectJsonBlobUrls(html)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : regexScan(html, M3U8_PATTERN)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : regexScan(html, MPD_PATTERN)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : regexScan(html, MP4_PATTERN)) {
            addFormat(formats, seen, url, formatId++);
        }
        for (String url : regexScan(html, WEBM_PATTERN)) {
            addFormat(formats, seen, url, formatId++);
        }
        if (extraRegexes != null) {
            for (String regex : extraRegexes) {
                Pattern pattern = Pattern.compile(regex);
                for (String url : regexScan(html, pattern)) {
                    addFormat(formats, seen, url, formatId++);
                }
            }
        }
        return formats;
    }

    public static VideoInfo buildVideoInfo(String pageUrl, String html, String id, List<String> extraRegexes) {
        PageMetadata meta = parseMetadata(html);
        List<Format> formats = findFormats(html, pageUrl, extraRegexes);
        if (formats.isEmpty()) {
            return null;
        }

        VideoInfo info = new VideoInfo();
        info.setId(id != null ? id : UrlUtils.filenameFromUrl(pageUrl));
        info.setTitle(meta.title() != null ? meta.title() : info.getId());
        info.setDescription(meta.description());
        info.setThumbnail(meta.thumbnail());
        info.setWebpageUrl(pageUrl);
        info.setUrl(pageUrl);
        info.setFormats(formats);
        info.setExt(formats.get(0).getExt());
        return info;
    }

    private static void addFormat(List<Format> formats, Set<String> seen, String url, int id) {
        String normalized = normalizeUrl(url);
        if (normalized == null || !seen.add(normalized)) {
            return;
        }
        Format format = new Format();
        format.setUrl(normalized);
        format.setFormatId(String.valueOf(id));
        format.setExt(guessExt(normalized));
        format.setProtocol(guessProtocol(normalized));
        format.setVcodec("unknown");
        format.setAcodec("unknown");
        formats.add(format);
    }

    private static String guessProtocol(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8")) {
            return "m3u8";
        }
        if (lower.contains(".mpd")) {
            return "http_dash_segments";
        }
        return "https";
    }

    private static String guessExt(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".m3u8")) {
            return "mp4";
        }
        if (lower.contains(".mpd")) {
            return "mp4";
        }
        return UrlUtils.determineExt(url, "mp4");
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        url = url.trim().replace("\\/", "/");
        if (url.startsWith("//")) {
            url = "https:" + url;
        }
        if (!url.startsWith("http")) {
            return null;
        }
        try {
            URI.create(url);
            return url;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> collectOpenGraphVideos(Document doc) {
        List<String> urls = new ArrayList<>();
        for (String property : List.of("og:video", "og:video:url", "og:video:secure_url")) {
            String value = metaContent(doc, property);
            if (value != null) {
                urls.add(value);
            }
        }
        return urls;
    }

    private static List<String> collectHtml5Sources(Document doc) {
        List<String> urls = new ArrayList<>();
        Elements videos = doc.select("video[src], video source[src], audio[src], audio source[src]");
        for (Element el : videos) {
            String src = el.attr("abs:src");
            if (!src.isBlank()) {
                urls.add(src);
            }
        }
        return urls;
    }

    private static List<String> collectJsonLdVideos(Document doc) {
        List<String> urls = new ArrayList<>();
        for (Element script : doc.select("script[type=application/ld+json]")) {
            try {
                JsonNode node = JsonUtils.parse(script.data());
                collectJsonLdNode(node, urls);
            } catch (Exception ignored) {
            }
        }
        return urls;
    }

    private static void collectJsonLdNode(JsonNode node, List<String> urls) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectJsonLdNode(child, urls);
            }
            return;
        }
        String type = JsonUtils.getText(node, "@type");
        if (type != null && type.toLowerCase().contains("video")) {
            String contentUrl = JsonUtils.getText(node, "contentUrl");
            if (contentUrl != null) {
                urls.add(contentUrl);
            }
            String embedUrl = JsonUtils.getText(node, "embedUrl");
            if (embedUrl != null) {
                urls.add(embedUrl);
            }
        }
        node.fields().forEachRemaining(entry -> collectJsonLdNode(entry.getValue(), urls));
    }

    private static List<String> collectJsonBlobUrls(String html) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = JSON_BLOB_PATTERN.matcher(html);
        while (matcher.find()) {
            try {
                JsonNode root = JsonUtils.parse(matcher.group(1));
                collectUrlsFromJson(root, urls, 0);
            } catch (Exception ignored) {
            }
        }
        return urls;
    }

    private static void collectUrlsFromJson(JsonNode node, List<String> urls, int depth) {
        if (node == null || depth > 12) {
            return;
        }
        if (node.isTextual()) {
            String text = node.asText();
            if (looksLikeMediaUrl(text)) {
                urls.add(text);
            }
            return;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                collectUrlsFromJson(child, urls, depth + 1);
            }
            return;
        }
        if (node.isObject()) {
            for (String key : List.of("url", "hls", "hlsUrl", "m3u8", "mp4", "videoUrl", "playUrl", "src")) {
                JsonNode value = node.get(key);
                if (value != null && value.isTextual() && looksLikeMediaUrl(value.asText())) {
                    urls.add(value.asText());
                }
            }
            node.fields().forEachRemaining(entry -> collectUrlsFromJson(entry.getValue(), urls, depth + 1));
        }
    }

    private static boolean looksLikeMediaUrl(String text) {
        if (text == null || text.length() < 10) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.startsWith("http")
                && (lower.contains(".m3u8") || lower.contains(".mpd") || lower.contains(".mp4")
                || lower.contains(".webm") || lower.contains("/manifest"));
    }

    private static List<String> regexScan(String text, Pattern pattern) {
        List<String> urls = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group());
        }
        return urls;
    }

    private static String metaContent(Document doc, String property) {
        Element el = doc.selectFirst("meta[property=" + property + "], meta[name=" + property + "]");
        return el != null ? el.attr("content") : null;
    }

    public record PageMetadata(String title, String description, String thumbnail) {
    }
}
