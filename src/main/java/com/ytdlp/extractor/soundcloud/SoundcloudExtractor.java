package com.ytdlp.extractor.soundcloud;

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

public class SoundcloudExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://(?:www\\.)?soundcloud\\.com/[^/]+/[^/?#]+";
    private static final Pattern CLIENT_ID = Pattern.compile("client_id=([a-zA-Z0-9]{32})");

    public SoundcloudExtractor() {
        super("soundcloud", "Soundcloud", VALID_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String page = downloadWebpage(url, null);
        String clientId = findClientId(page);
        if (clientId == null) {
            clientId = findClientId(downloadWebpage("https://soundcloud.com/", null));
        }
        if (clientId == null) {
            throw new ExtractorException("Could not find SoundCloud client_id", true);
        }

        String resolveUrl = "https://api-v2.soundcloud.com/resolve?url="
                + java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8)
                + "&client_id=" + clientId;
        String json = ydl.getRequestDirector().downloadString(new Request(resolveUrl));
        JsonNode track = JsonUtils.parse(json);

        VideoInfo info = new VideoInfo();
        info.setId(String.valueOf(JsonUtils.getLong(track, "id")));
        info.setTitle(JsonUtils.getText(track, "title"));
        info.setDescription(JsonUtils.getText(track, "description"));
        info.setDuration(JsonUtils.getLong(track, "duration") != null
                ? JsonUtils.getLong(track, "duration") / 1000 : null);
        info.setThumbnail(JsonUtils.getText(track, "artwork_url"));
        info.setWebpageUrl(url);
        info.setUrl(url);

        List<Format> formats = new ArrayList<>();
        JsonNode transcodings = track.path("media").path("transcodings");
        if (transcodings.isArray()) {
            int idx = 0;
            for (JsonNode t : transcodings) {
                String protocol = JsonUtils.getText(t, "format", "protocol");
                if (!"progressive".equals(protocol) && !"hls".equals(protocol)) {
                    continue;
                }
                String streamUrl = JsonUtils.getText(t, "url") + "?client_id=" + clientId;
                String streamJson = ydl.getRequestDirector().downloadString(new Request(streamUrl));
                String mediaUrl = JsonUtils.getText(JsonUtils.parse(streamJson), "url");
                if (mediaUrl != null) {
                    Format format = new Format();
                    format.setFormatId(String.valueOf(idx++));
                    format.setUrl(mediaUrl);
                    format.setExt("hls".equals(protocol) ? "mp4" : "mp3");
                    format.setProtocol("hls".equals(protocol) ? "m3u8" : "https");
                    format.setAcodec("mp3");
                    formats.add(format);
                }
            }
        }

        if (formats.isEmpty()) {
            throw new ExtractorException("No formats found for SoundCloud track", true);
        }
        info.setFormats(formats);
        info.setExt("mp3");
        return ExtractorResult.video(info);
    }

    private static String findClientId(String html) {
        Matcher matcher = CLIENT_ID.matcher(html);
        return matcher.find() ? matcher.group(1) : null;
    }
}
