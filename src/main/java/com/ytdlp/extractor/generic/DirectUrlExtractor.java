package com.ytdlp.extractor.generic;

import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.util.UrlUtils;

import java.util.List;
import java.util.Set;

/**
 * Extractor for direct media URLs (mp4, m3u8, mpd, etc.).
 */
public class DirectUrlExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://.+";
    private static final Set<String> MEDIA_EXTENSIONS = Set.of(
            "mp4", "webm", "mkv", "avi", "mov", "flv", "wmv", "m4v",
            "mp3", "m4a", "aac", "opus", "ogg", "wav", "flac",
            "ts", "m2ts", "f4v", "3gp", "mpeg", "mpg");
    private static final Set<String> MEDIA_PATH_HINTS = Set.of(".m3u8", ".mpd", "/manifest");

    public DirectUrlExtractor() {
        super("generic", "Generic", VALID_URL);
    }

    public static boolean looksLikeDirectMedia(String url) {
        String lower = url.toLowerCase();
        for (String hint : MEDIA_PATH_HINTS) {
            if (lower.contains(hint)) {
                return true;
            }
        }
        for (String ext : MEDIA_EXTENSIONS) {
            if (lower.matches(".*\\." + ext + "([?#].*)?$")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean suitable(String url) {
        return super.suitable(url) && looksLikeDirectMedia(url);
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String ext = UrlUtils.determineExt(url, "mp4");
        String filename = UrlUtils.filenameFromUrl(url);
        String id = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;

        Format format = new Format();
        format.setUrl(url);
        format.setExt(ext);
        format.setFormatId("0");
        format.setProtocol(guessProtocol(url));
        format.setVcodec("unknown");
        format.setAcodec("unknown");

        try {
            Request headRequest = new Request(url, "HEAD", null, null);
            try (var response = ydl.getRequestDirector().send(headRequest)) {
                String contentType = response.getHeader("Content-Type");
                if (contentType != null) {
                    format.setExt(UrlUtils.mimeToExt(contentType));
                }
                String contentLength = response.getHeader("Content-Length");
                if (contentLength != null) {
                    try {
                        format.setFilesize(Long.parseLong(contentLength));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            if (!ydl.getOptions().isNoWarnings()) {
                ydl.reportWarning("Could not probe URL headers: " + e.getMessage());
            }
        }

        VideoInfo info = new VideoInfo();
        info.setId(id);
        info.setTitle(id);
        info.setUrl(url);
        info.setExt(format.getExt());
        info.setFormats(List.of(format));
        return ExtractorResult.video(info);
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
}
