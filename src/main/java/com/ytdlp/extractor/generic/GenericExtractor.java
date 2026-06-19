package com.ytdlp.extractor.generic;

import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.util.UrlUtils;

import java.util.List;

/**
 * Generic extractor for direct media URLs.
 * Ported from yt-dlp's GenericIE.
 */
public class GenericExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://.+";

    public GenericExtractor() {
        super("generic", "Generic", VALID_URL);
    }

    @Override
    protected VideoInfo realExtract(String url) {
        String ext = UrlUtils.determineExt(url, "mp4");
        String filename = UrlUtils.filenameFromUrl(url);
        String id = filename.contains(".") ? filename.substring(0, filename.lastIndexOf('.')) : filename;

        Format format = new Format();
        format.setUrl(url);
        format.setExt(ext);
        format.setFormatId("0");
        format.setProtocol("https");
        format.setVcodec("unknown");
        format.setAcodec("unknown");

        // Try HEAD request to get content type
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
        return info;
    }
}
