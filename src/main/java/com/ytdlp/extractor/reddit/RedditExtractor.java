package com.ytdlp.extractor.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.util.JsonUtils;
import com.ytdlp.util.UrlUtils;

import java.util.ArrayList;
import java.util.List;

public class RedditExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://(?:www\\.|old\\.|new\\.)?reddit\\.com/(?:r|u|user)/[^/]+/comments/[^/]+";

    public RedditExtractor() {
        super("reddit", "Reddit", VALID_URL);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String jsonUrl = url.endsWith("/") ? url + ".json" : url + ".json";
        String json = ydl.getRequestDirector().downloadString(new Request(jsonUrl));
        JsonNode root = JsonUtils.parse(json);

        JsonNode post = root.path(0).path("data").path("children").path(0).path("data");
        if (post.isMissingNode()) {
            throw new ExtractorException("Could not parse Reddit post: " + url);
        }

        VideoInfo info = new VideoInfo();
        info.setId(JsonUtils.getText(post, "id"));
        info.setTitle(JsonUtils.getText(post, "title"));
        info.setWebpageUrl(url);
        info.setUrl(url);
        info.setThumbnail(JsonUtils.getText(post, "thumbnail"));

        List<Format> formats = new ArrayList<>();
        JsonNode media = post.path("secure_media").path("reddit_video");
        if (media.isMissingNode()) {
            media = post.path("media").path("reddit_video");
        }
        if (!media.isMissingNode()) {
            String fallback = JsonUtils.getText(media, "fallback_url");
            if (fallback != null) {
                Format format = new Format();
                format.setFormatId("fallback");
                format.setUrl(fallback);
                format.setExt("mp4");
                format.setProtocol("https");
                format.setHeight(JsonUtils.getInt(media, "height"));
                format.setWidth(JsonUtils.getInt(media, "width"));
                formats.add(format);
            }
            String hls = JsonUtils.getText(media, "hls_url");
            if (hls != null) {
                Format format = new Format();
                format.setFormatId("hls");
                format.setUrl(hls);
                format.setExt("mp4");
                format.setProtocol("m3u8");
                formats.add(format);
            }
        }

        String postUrl = JsonUtils.getText(post, "url");
        if (formats.isEmpty() && postUrl != null && DirectMedia.isDirect(postUrl)) {
            Format format = new Format();
            format.setFormatId("0");
            format.setUrl(postUrl);
            format.setExt(UrlUtils.determineExt(postUrl, "mp4"));
            format.setProtocol("https");
            formats.add(format);
        }

        if (formats.isEmpty()) {
            throw new ExtractorException("No video found in Reddit post: " + url, true);
        }
        info.setFormats(formats);
        info.setExt("mp4");
        return ExtractorResult.video(info);
    }

    private static final class DirectMedia {
        static boolean isDirect(String url) {
            String lower = url.toLowerCase();
            return lower.contains(".mp4") || lower.contains(".webm") || lower.contains(".m3u8")
                    || lower.contains("v.redd.it") || lower.contains("i.redd.it");
        }
    }
}
