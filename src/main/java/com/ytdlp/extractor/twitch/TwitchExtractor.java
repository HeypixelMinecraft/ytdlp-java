package com.ytdlp.extractor.twitch;

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

public class TwitchExtractor extends InfoExtractor {
    private static final String CLIP_URL = "(?i)https?://(?:clips\\.twitch\\.tv/|(?:www\\.)?twitch\\.tv/[^/]+/clip/)";
    private static final String VOD_URL = "(?i)https?://(?:www\\.)?twitch\\.tv/videos/(\\d+)";
    private static final Pattern CLIP_SLUG = Pattern.compile(
            "(?i)https?://clips\\.twitch\\.tv/(?:embed\\?clip=)?([^/?#]+)|"
                    + "https?://(?:www\\.)?twitch\\.tv/[^/]+/clip/([^/?#]+)");
    private static final Pattern VOD_ID = Pattern.compile(VOD_URL);

    public TwitchExtractor() {
        super("twitch", "Twitch", CLIP_URL + "|" + VOD_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        Matcher vodMatcher = VOD_ID.matcher(url);
        if (vodMatcher.find()) {
            return extractVod(vodMatcher.group(1), url);
        }
        Matcher clipMatcher = CLIP_SLUG.matcher(url);
        if (clipMatcher.find()) {
            String slug = clipMatcher.group(1) != null ? clipMatcher.group(1) : clipMatcher.group(2);
            return extractClip(slug, url);
        }
        throw new ExtractorException("Unsupported Twitch URL: " + url);
    }

    private ExtractorResult extractClip(String slug, String webpageUrl) {
        String gqlUrl = "https://gql.twitch.tv/gql";
        String query = """
                {"query":"query{clip(slug:\\"%s\\"){title,thumbnailURL,durationSeconds,broadcaster{displayName},videoQualities{sourceURL,quality}}}"}
                """.formatted(slug);
        Request request = new Request(gqlUrl, "POST", query.getBytes(),
                java.util.Map.of("Client-Id", "kimne78kx3ncx6brgo4mv6wki5h1ko", "Content-Type", "application/json"));
        String json = ydl.getRequestDirector().downloadString(request);
        JsonNode clip = JsonUtils.parse(json).path("data").path("clip");

        VideoInfo info = new VideoInfo();
        info.setId(slug);
        info.setTitle(JsonUtils.getText(clip, "title"));
        info.setDuration(JsonUtils.getLong(clip, "durationSeconds"));
        info.setThumbnail(JsonUtils.getText(clip, "thumbnailURL"));
        info.setWebpageUrl(webpageUrl);
        info.setUrl(webpageUrl);

        List<Format> formats = new ArrayList<>();
        JsonNode qualities = clip.path("videoQualities");
        if (qualities.isArray()) {
            int idx = 0;
            for (JsonNode q : qualities) {
                String source = JsonUtils.getText(q, "sourceURL");
                if (source != null) {
                    Format format = new Format();
                    format.setFormatId(String.valueOf(idx++));
                    format.setUrl(source);
                    format.setExt("mp4");
                    format.setProtocol("https");
                    format.setFormatNote(JsonUtils.getText(q, "quality"));
                    formats.add(format);
                }
            }
        }
        if (formats.isEmpty()) {
            throw new ExtractorException("No formats found for Twitch clip: " + slug);
        }
        info.setFormats(formats);
        info.setExt("mp4");
        return ExtractorResult.video(info);
    }

    private ExtractorResult extractVod(String videoId, String webpageUrl) {
        String gqlUrl = "https://gql.twitch.tv/gql";
        String query = """
                {"query":"query{video(id:\\"%s\\"){title,previewThumbnailURL,lengthSeconds,playbackAccessToken(signature,value),owner{displayName}}}"}
                """.formatted(videoId);
        Request request = new Request(gqlUrl, "POST", query.getBytes(),
                java.util.Map.of("Client-Id", "kimne78kx3ncx6brgo4mv6wki5h1ko", "Content-Type", "application/json"));
        String json = ydl.getRequestDirector().downloadString(request);
        JsonNode video = JsonUtils.parse(json).path("data").path("video");

        VideoInfo info = new VideoInfo();
        info.setId(videoId);
        info.setTitle(JsonUtils.getText(video, "title"));
        info.setDuration(JsonUtils.getLong(video, "lengthSeconds"));
        info.setThumbnail(JsonUtils.getText(video, "previewThumbnailURL"));
        info.setWebpageUrl(webpageUrl);
        info.setUrl(webpageUrl);

        JsonNode token = video.path("playbackAccessToken");
        String sig = JsonUtils.getText(token, "signature");
        String value = JsonUtils.getText(token, "value");
        if (sig == null || value == null) {
            throw new ExtractorException("No playback token for Twitch VOD: " + videoId);
        }

        String usherUrl = "https://usher.ttvnw.net/vod/%s.m3u8?nauthsig=%s&nauth=%s&allow_source=true&player=twitchweb"
                .formatted(videoId, sig, java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));

        Format format = new Format();
        format.setFormatId("0");
        format.setUrl(usherUrl);
        format.setExt("mp4");
        format.setProtocol("m3u8");

        info.setFormats(List.of(format));
        info.setExt("mp4");
        return ExtractorResult.video(info);
    }
}
