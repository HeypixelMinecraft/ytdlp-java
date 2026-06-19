package com.ytdlp.extractor.dailymotion;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DailymotionExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://(?:www\\.)?dailymotion\\.com/(?:video|embed/video)/([^/?#]+)";
    private static final Pattern ID_PATTERN = Pattern.compile(VALID_URL);

    public DailymotionExtractor() {
        super("dailymotion", "Dailymotion", VALID_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        Matcher matcher = ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new ExtractorException("Invalid Dailymotion URL: " + url);
        }
        String videoId = matcher.group(1);

        String apiUrl = "https://www.dailymotion.com/player/metadata/video/" + videoId;
        String json = ydl.getRequestDirector().downloadString(new Request(apiUrl));
        JsonNode root = JsonUtils.parse(json);

        VideoInfo info = new VideoInfo();
        info.setId(videoId);
        info.setTitle(JsonUtils.getText(root, "title"));
        info.setDescription(JsonUtils.getText(root, "description"));
        info.setDuration(JsonUtils.getLong(root, "duration"));
        info.setThumbnail(JsonUtils.getText(root, "thumbnail_url"));
        info.setWebpageUrl("https://www.dailymotion.com/video/" + videoId);
        info.setUrl(info.getWebpageUrl());

        List<Format> formats = new ArrayList<>();
        JsonNode qualities = root.path("qualities");
        if (qualities.isObject()) {
            qualities.fields().forEachRemaining(entry -> {
                JsonNode q = entry.getValue();
                JsonNode arr = q.path("type").isMissingNode() ? q : q;
                if (arr.isArray()) {
                    for (JsonNode stream : arr) {
                        addStream(formats, stream, entry.getKey());
                    }
                } else if (q.isArray()) {
                    for (JsonNode stream : q) {
                        addStream(formats, stream, entry.getKey());
                    }
                }
            });
        }
        JsonNode streamUrl = root.get("stream_url");
        if (streamUrl != null && streamUrl.isTextual()) {
            Format format = new Format();
            format.setFormatId("0");
            format.setUrl(streamUrl.asText());
            format.setExt("mp4");
            format.setProtocol("https");
            formats.add(format);
        }

        if (formats.isEmpty()) {
            throw new ExtractorException("No formats found for Dailymotion video: " + videoId);
        }
        info.setFormats(formats);
        info.setExt("mp4");
        return ExtractorResult.video(info);
    }

    private static void addStream(List<Format> formats, JsonNode stream, String quality) {
        String url = JsonUtils.getText(stream, "url");
        if (url == null) {
            return;
        }
        Format format = new Format();
        format.setFormatId(quality);
        format.setUrl(url);
        format.setExt("mp4");
        format.setProtocol(url.contains(".m3u8") ? "m3u8" : "https");
        format.setWidth(JsonUtils.getInt(stream, "width"));
        format.setHeight(JsonUtils.getInt(stream, "height"));
        formats.add(format);
    }
}
