package com.ytdlp.extractor.generic;

import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.extractor.common.WebpageMediaFinder;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.UrlUtils;

import java.net.URI;
import java.util.Set;

/**
 * Universal Java fallback for arbitrary HTTP(S) pages.
 * Parses embedded media via OpenGraph, HTML5, JSON-LD, and regex scanning.
 */
public class WebpageExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://.+";
    private static final Set<String> YOUTUBE_HOSTS = Set.of(
            "youtube.com", "www.youtube.com", "youtu.be", "m.youtube.com", "music.youtube.com");

    public WebpageExtractor() {
        super("webpage", "Webpage", VALID_URL);
    }

    @Override
    public boolean suitable(String url) {
        if (!super.suitable(url)) {
            return false;
        }
        try {
            String host = URI.create(url).getHost();
            if (host != null && YOUTUBE_HOSTS.contains(host.toLowerCase())) {
                return false;
            }
        } catch (Exception ignored) {
        }
        return !DirectUrlExtractor.looksLikeDirectMedia(url);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String html = downloadWebpage(url, null);
        String id = UrlUtils.filenameFromUrl(url);
        VideoInfo info = WebpageMediaFinder.buildVideoInfo(url, html, id, null);
        if (info == null || info.getFormats().isEmpty()) {
            throw new ExtractorException("No media URLs found in webpage: " + url, true);
        }
        return ExtractorResult.video(info);
    }
}
