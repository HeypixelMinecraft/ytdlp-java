package com.ytdlp.extractor.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.JsonUtils;
import com.ytdlp.util.UrlUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube video extractor using InnerTube API with multi-client fallback.
 * Ported from yt-dlp's YoutubeIE / YoutubeBaseInfoExtractor.
 */
public class YoutubeExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)(?:https?://)?(?:www\\.)?(?:youtube\\.com/(?:watch\\?v=|embed/|shorts/|live/)|youtu\\.be/|m\\.youtube\\.com/watch\\?v=)([0-9A-Za-z_-]{11})";
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(VALID_URL);
    private static final String INNERTUBE_API = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false";
    private static final Pattern INITIAL_PLAYER_RESPONSE_RE = Pattern.compile(
            "ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*;");

    private static final List<InnertubeClient> CLIENT_FALLBACK = List.of(
            InnertubeClient.androidVr(),
            InnertubeClient.ios(),
            InnertubeClient.android(),
            InnertubeClient.tv(),
            InnertubeClient.webSafari()
    );

    public YoutubeExtractor() {
        super("youtube", "Youtube", VALID_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String videoId = extractVideoId(url);
        if (videoId == null) {
            throw new ExtractorException("Could not extract video ID from URL: " + url);
        }

        JsonNode playerResponse = fetchBestPlayerResponse(videoId);
        checkPlayability(playerResponse, videoId);

        VideoInfo info = buildVideoInfo(videoId, playerResponse);
        List<Format> formats = extractFormats(playerResponse);
        if (formats.isEmpty()) {
            throw new ExtractorException(
                    "No downloadable formats found. Try --cookies or check if the video requires authentication.",
                    true);
        }
        info.setFormats(formats);
        return ExtractorResult.video(info);
    }

    private JsonNode fetchBestPlayerResponse(String videoId) {
        String webpageUrl = "https://www.youtube.com/watch?v=" + videoId;
        JsonNode fromWebpage = tryExtractFromWebpage(webpageUrl, videoId);
        if (fromWebpage != null && hasFormats(fromWebpage)) {
            return fromWebpage;
        }

        ExtractorException lastError = null;
        for (InnertubeClient client : CLIENT_FALLBACK) {
            try {
                JsonNode response = fetchPlayerResponse(videoId, client);
                String status = JsonUtils.getText(response, "playabilityStatus", "status");
                if ("OK".equals(status) || "LIVE_STREAM_OFFLINE".equals(status)) {
                    if (hasFormats(response) || fromWebpage == null) {
                        return response;
                    }
                }
                if (fromWebpage != null) {
                    return fromWebpage;
                }
                lastError = new ExtractorException(
                        "Client " + client.name() + ": " + JsonUtils.getText(response, "playabilityStatus", "reason"),
                        true);
            } catch (ExtractorException e) {
                lastError = e;
            }
        }

        if (fromWebpage != null) {
            return fromWebpage;
        }
        throw lastError != null ? lastError
                : new ExtractorException("Failed to fetch player response for " + videoId);
    }

    private JsonNode tryExtractFromWebpage(String webpageUrl, String videoId) {
        try {
            String webpage = downloadWebpage(webpageUrl, videoId);
            String json = JsonUtils.searchJson(INITIAL_PLAYER_RESPONSE_RE, webpage, "initial player response");
            if (json != null) {
                return JsonUtils.parse(json);
            }
        } catch (Exception e) {
            ydl.reportWarning("Could not parse webpage player response: " + e.getMessage());
        }
        return null;
    }

    private boolean hasFormats(JsonNode playerResponse) {
        JsonNode streamingData = playerResponse.get("streamingData");
        if (streamingData == null) {
            return false;
        }
        return (streamingData.has("formats") && streamingData.get("formats").size() > 0)
                || (streamingData.has("adaptiveFormats") && streamingData.get("adaptiveFormats").size() > 0);
    }

    private String extractVideoId(String url) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (url.matches("[0-9A-Za-z_-]{11}")) {
            return url;
        }
        return null;
    }

    private JsonNode fetchPlayerResponse(String videoId, InnertubeClient client) {
        Map<String, Object> clientMap = new LinkedHashMap<>();
        clientMap.put("clientName", client.clientName());
        clientMap.put("clientVersion", client.clientVersion());
        clientMap.put("userAgent", client.userAgent());
        clientMap.put("hl", "en");
        clientMap.put("timeZone", "UTC");
        clientMap.put("utcOffsetMinutes", 0);
        if (client.extraClientFields() != null) {
            clientMap.putAll(client.extraClientFields());
        }

        Map<String, Object> innerContext = new HashMap<>();
        innerContext.put("client", clientMap);

        Map<String, Object> body = new HashMap<>();
        body.put("context", innerContext);
        body.put("videoId", videoId);

        String jsonBody;
        try {
            jsonBody = JsonUtils.mapper().writeValueAsString(body);
        } catch (Exception e) {
            throw new ExtractorException("Failed to build player request", e);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-YouTube-Client-Name", String.valueOf(client.clientId()));
        headers.put("X-YouTube-Client-Version", client.clientVersion());
        headers.put("User-Agent", client.userAgent());
        headers.put("Origin", "https://www.youtube.com");

        String response = downloadJson(INNERTUBE_API, jsonBody.getBytes(StandardCharsets.UTF_8), headers);
        return JsonUtils.parse(response);
    }

    private void checkPlayability(JsonNode playerResponse, String videoId) {
        JsonNode playability = playerResponse.get("playabilityStatus");
        if (playability == null) {
            return;
        }
        String status = JsonUtils.getText(playability, "status");
        if ("OK".equals(status) || "LIVE_STREAM_OFFLINE".equals(status)) {
            return;
        }
        String reason = JsonUtils.getText(playability, "reason");
        throw new ExtractorException(
                "Video " + videoId + " is not playable: " + (reason != null ? reason : status),
                true);
    }

    private VideoInfo buildVideoInfo(String videoId, JsonNode playerResponse) {
        VideoInfo info = new VideoInfo();
        info.setId(videoId);
        info.setWebpageUrl("https://www.youtube.com/watch?v=" + videoId);

        JsonNode videoDetails = playerResponse.get("videoDetails");
        if (videoDetails != null) {
            info.setTitle(JsonUtils.getText(videoDetails, "title"));
            info.setDescription(JsonUtils.getText(videoDetails, "shortDescription"));
            info.setChannel(JsonUtils.getText(videoDetails, "author"));
            info.setChannelId(JsonUtils.getText(videoDetails, "channelId"));
            info.setDuration(JsonUtils.getLong(videoDetails, "lengthSeconds"));
            info.setViewCount(JsonUtils.getLong(videoDetails, "viewCount"));

            JsonNode thumbnails = videoDetails.get("thumbnail");
            if (thumbnails != null && thumbnails.has("thumbnails")) {
                JsonNode thumbList = thumbnails.get("thumbnails");
                if (thumbList.isArray() && !thumbList.isEmpty()) {
                    info.setThumbnail(thumbList.get(thumbList.size() - 1).path("url").asText(null));
                }
            }
        }
        return info;
    }

    private List<Format> extractFormats(JsonNode playerResponse) {
        List<Format> formats = new ArrayList<>();
        JsonNode streamingData = playerResponse.get("streamingData");
        if (streamingData == null) {
            return formats;
        }

        addFormatStreams(formats, streamingData.get("formats"));
        addFormatStreams(formats, streamingData.get("adaptiveFormats"));
        return formats;
    }

    private void addFormatStreams(List<Format> formats, JsonNode streams) {
        if (streams == null || !streams.isArray()) {
            return;
        }
        for (JsonNode stream : streams) {
            String url = JsonUtils.getText(stream, "url");
            if (url == null) {
                continue;
            }

            Format format = new Format();
            format.setUrl(url);
            format.setFormatId(JsonUtils.getText(stream, "itag"));
            format.setFormatNote(buildFormatNote(stream));
            format.setWidth(JsonUtils.getInt(stream, "width"));
            format.setHeight(JsonUtils.getInt(stream, "height"));
            format.setFps(JsonUtils.getInt(stream, "fps"));
            format.setTbr(JsonUtils.getInt(stream, "averageBitrate"));
            format.setFilesize(JsonUtils.getLong(stream, "contentLength"));

            String mimeType = JsonUtils.getText(stream, "mimeType");
            format.setExt(UrlUtils.mimeToExt(mimeType));

            if (mimeType != null) {
                if (mimeType.startsWith("audio/") || mimeType.contains("audio")) {
                    format.setVcodec("none");
                    format.setAcodec(extractCodec(mimeType));
                } else {
                    format.setVcodec(extractCodec(mimeType));
                    format.setAcodec("none");
                    if (mimeType.contains("mp4a")) {
                        format.setAcodec("mp4a.40.2");
                    }
                }
            }

            if (stream.has("audioQuality") && stream.has("qualityLabel")) {
                format.setAcodec(format.getAcodec() != null && !"none".equals(format.getAcodec())
                        ? format.getAcodec() : "mp4a.40.2");
                format.setVcodec(format.getVcodec() != null && !"none".equals(format.getVcodec())
                        ? format.getVcodec() : "avc1");
            }

            formats.add(format);
        }
    }

    private String buildFormatNote(JsonNode stream) {
        String quality = JsonUtils.getText(stream, "qualityLabel");
        if (quality != null) {
            return quality;
        }
        String audioQuality = JsonUtils.getText(stream, "audioQuality");
        if (audioQuality != null) {
            return audioQuality.replace("audio_quality_", "");
        }
        return JsonUtils.getText(stream, "quality");
    }

    private String extractCodec(String mimeType) {
        int idx = mimeType.indexOf("codecs=\"");
        if (idx >= 0) {
            int end = mimeType.indexOf('"', idx + 8);
            if (end > idx) {
                String codecs = mimeType.substring(idx + 8, end);
                return codecs.split(",")[0].trim();
            }
        }
        return mimeType.contains("audio") ? "none" : "unknown";
    }
}
