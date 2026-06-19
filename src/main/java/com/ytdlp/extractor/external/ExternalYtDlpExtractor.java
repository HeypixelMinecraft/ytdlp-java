package com.ytdlp.extractor.external;

import com.ytdlp.bridge.YtDlpBridge;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;

/**
 * Universal fallback extractor delegating to the official yt-dlp binary.
 * Provides access to 1000+ site extractors maintained by the yt-dlp project.
 */
public class ExternalYtDlpExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://.+";
    private YtDlpBridge bridge;

    public ExternalYtDlpExtractor() {
        super("yt-dlp (external)", "ExternalYtDlp", VALID_URL);
    }

    @Override
    public void setDownloader(com.ytdlp.YoutubeDL ydl) {
        super.setDownloader(ydl);
        this.bridge = new YtDlpBridge(ydl.getOptions());
    }

    @Override
    public boolean suitable(String url) {
        if (!super.suitable(url)) {
            return false;
        }
        if (ydl == null || !ydl.getOptions().isExternalYtDlpEnabled()) {
            return false;
        }
        if (bridge == null) {
            bridge = new YtDlpBridge(ydl.getOptions());
        }
        return bridge.isAvailable();
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        if (bridge == null || !bridge.isAvailable()) {
            throw new ExtractorException(
                    "External yt-dlp is not available. Install yt-dlp and ensure it is on PATH, "
                            + "or set --yt-dlp-location / path via options.setExternalYtDlpPath().",
                    true);
        }
        ExtractorResult result = bridge.extract(url);
        if (result.isVideo() && result.getVideo() != null) {
            result.getVideo().setExtractorKey(getIeKey());
            result.getVideo().setExtractor(getIeName());
        }
        if (result.isPlaylist() && result.getPlaylist() != null) {
            result.getPlaylist().setExtractorKey(getIeKey());
            result.getPlaylist().setExtractor(getIeName());
        }
        return result;
    }

    public YtDlpBridge getBridge() {
        return bridge;
    }
}
