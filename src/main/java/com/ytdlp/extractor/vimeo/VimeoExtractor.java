package com.ytdlp.extractor.vimeo;

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

public class VimeoExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://(?:www\\.|player\\.)?vimeo\\.com/(?:video/)?(\\d+)";
    private static final Pattern ID_PATTERN = Pattern.compile(VALID_URL);

    public VimeoExtractor() {
        super("vimeo", "Vimeo", VALID_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        Matcher matcher = ID_PATTERN.matcher(url);
        if (!matcher.find()) {
            throw new ExtractorException("Invalid Vimeo URL: " + url);
        }
        String videoId = matcher.group(1);

        String configUrl = "https://player.vimeo.com/video/" + videoId + "/config";
        String json = ydl.getRequestDirector().downloadString(new Request(configUrl));
        JsonNode root = JsonUtils.parse(json);

        VideoInfo info = new VideoInfo();
        info.setId(videoId);
        info.setTitle(JsonUtils.getText(root, "video", "title"));
        info.setDescription(JsonUtils.getText(root, "video", "description"));
        info.setDuration(JsonUtils.getLong(root, "video", "duration"));
        info.setThumbnail(JsonUtils.getText(root, "video", "thumbs", "base"));
        info.setWebpageUrl("https://vimeo.com/" + videoId);
        info.setUrl(info.getWebpageUrl());

        List<Format> formats = new ArrayList<>();
        JsonNode progressive = root.path("request").path("files").path("progressive");
        if (progressive.isArray()) {
            int idx = 0;
            for (JsonNode f : progressive) {
                Format format = new Format();
                format.setFormatId(String.valueOf(idx++));
                format.setUrl(JsonUtils.getText(f, "url"));
                format.setExt(JsonUtils.getText(f, "mime") != null
                        ? JsonUtils.getText(f, "mime").contains("webm") ? "webm" : "mp4" : "mp4");
                format.setWidth(JsonUtils.getInt(f, "width"));
                format.setHeight(JsonUtils.getInt(f, "height"));
                format.setProtocol("https");
                format.setVcodec("avc1");
                format.setAcodec("mp4a");
                formats.add(format);
            }
        }
        JsonNode hls = root.path("request").path("files").path("hls");
        String hlsUrl = JsonUtils.getText(hls, "cdns", "akamai", "url");
        if (hlsUrl == null) {
            hlsUrl = JsonUtils.getText(hls, "cdns", "fastly", "url");
        }
        if (hlsUrl != null) {
            Format hlsFormat = new Format();
            hlsFormat.setFormatId("hls");
            hlsFormat.setUrl(hlsUrl);
            hlsFormat.setExt("mp4");
            hlsFormat.setProtocol("m3u8");
            hlsFormat.setVcodec("avc1");
            hlsFormat.setAcodec("mp4a");
            formats.add(hlsFormat);
        }

        if (formats.isEmpty()) {
            throw new ExtractorException("No formats found for Vimeo video: " + videoId);
        }
        info.setFormats(formats);
        info.setExt("mp4");
        return ExtractorResult.video(info);
    }
}
