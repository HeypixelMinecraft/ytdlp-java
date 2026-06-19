package com.ytdlp.extractor.bilibili;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Basic Bilibili extractor via inline player data from the watch page.
 */
public class BilibiliExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://(?:www\\.|m\\.)?bilibili\\.com/video/(?:BV[^/?#]+|av\\d+)";
    private static final Pattern BV_PATTERN = Pattern.compile("(?i)BV[0-9A-Za-z]+");
    private static final Pattern AID_PATTERN = Pattern.compile("(?i)av(\\d+)");

    public BilibiliExtractor() {
        super("bilibili", "Bilibili", VALID_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String html = downloadWebpage(url, null);

        String title = extractBetween(html, "\"title\":\"", "\"");
        if (title == null) {
            title = extractBetween(html, "<title>", "</title>");
        }

        VideoInfo info = new VideoInfo();
        info.setTitle(title != null ? title.replace("- bilibili", "").trim() : "bilibili");
        info.setWebpageUrl(url);
        info.setUrl(url);

        Matcher bv = BV_PATTERN.matcher(url);
        if (bv.find()) {
            info.setId(bv.group());
        } else {
            Matcher av = AID_PATTERN.matcher(url);
            info.setId(av.find() ? av.group(1) : url);
        }

        List<Format> formats = new ArrayList<>();
        String playUrl = extractBetween(html, "\"playUrl\":\"", "\"");
        if (playUrl != null) {
            playUrl = playUrl.replace("\\u002F", "/").replace("\\/", "/");
            addFormat(formats, playUrl, "0");
        }
        String dashVideo = extractBetween(html, "\"video\":\"", "\"");
        if (dashVideo != null) {
            dashVideo = dashVideo.replace("\\u002F", "/").replace("\\/", "/");
            addFormat(formats, dashVideo, "dash-video");
        }
        String dashAudio = extractBetween(html, "\"audio\":\"", "\"");
        if (dashAudio != null) {
            dashAudio = dashAudio.replace("\\u002F", "/").replace("\\/", "/");
            addFormat(formats, dashAudio, "dash-audio");
        }

        if (formats.isEmpty()) {
            JsonNode initial = extractInitialState(html);
            if (initial != null) {
                collectFromJson(initial, formats);
            }
        }

        if (formats.isEmpty()) {
            throw new ExtractorException("No formats found for Bilibili video. Try cookies or external yt-dlp.", true);
        }
        info.setFormats(formats);
        info.setExt("mp4");
        return ExtractorResult.video(info);
    }

    private static void addFormat(List<Format> formats, String url, String id) {
        Format format = new Format();
        format.setFormatId(id);
        format.setUrl(url);
        format.setExt(url.contains(".m4s") ? "m4s" : "mp4");
        format.setProtocol(url.contains(".m3u8") ? "m3u8" : "https");
        formats.add(format);
    }

    private static JsonNode extractInitialState(String html) {
        String marker = "window.__playinfo__=";
        int start = html.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = html.indexOf("</script>", start);
        if (end < 0) {
            return null;
        }
        try {
            return JsonUtils.parse(html.substring(start, end).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static void collectFromJson(JsonNode root, List<Format> formats) {
        JsonNode dash = root.path("data").path("dash");
        JsonNode video = dash.path("video");
        if (video.isArray()) {
            int idx = 0;
            for (JsonNode v : video) {
                String base = JsonUtils.getText(v, "baseUrl");
                if (base != null) {
                    addFormat(formats, base, "v" + idx++);
                }
            }
        }
        JsonNode audio = dash.path("audio");
        if (audio.isArray()) {
            int idx = 0;
            for (JsonNode a : audio) {
                String base = JsonUtils.getText(a, "baseUrl");
                if (base != null) {
                    addFormat(formats, base, "a" + idx++);
                }
            }
        }
    }

    private static String extractBetween(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) {
            return null;
        }
        s += start.length();
        int e = text.indexOf(end, s);
        return e < 0 ? null : text.substring(s, e);
    }
}
