package com.ytdlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ytdlp.bridge.YtDlpBridge;
import com.ytdlp.downloader.DownloaderFactory;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.exception.YtDlpException;
import com.ytdlp.extractor.ExtractorRegistry;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.format.FormatSelector;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.PlaylistInfo;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.RequestDirector;
import com.ytdlp.util.UrlUtils;
import okhttp3.OkHttpClient;

import java.nio.file.Path;
import java.util.List;

/**
 * Core orchestrator ported from yt-dlp's YoutubeDL.
 */
public class YoutubeDL implements AutoCloseable {
    private final YoutubeDLOptions options;
    private final ExtractorRegistry registry;
    private final RequestDirector requestDirector;
    private final FormatSelector formatSelector;
    private final DownloaderFactory downloaderFactory;
    private final ObjectMapper jsonMapper;
    private YtDlpBridge externalBridge;

    public YoutubeDL(YoutubeDLOptions options) {
        this(options, null);
    }

    public YoutubeDL(YoutubeDLOptions options, OkHttpClient httpClient) {
        this.options = options;
        this.registry = new ExtractorRegistry(options.isExternalYtDlpEnabled());
        this.requestDirector = new RequestDirector(options, httpClient);
        this.formatSelector = new FormatSelector();
        this.downloaderFactory = new DownloaderFactory(this);
        this.jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        for (InfoExtractor ie : registry.getExtractors()) {
            ie.setDownloader(this);
        }
    }

    public YoutubeDLOptions getOptions() {
        return options;
    }

    public RequestDirector getRequestDirector() {
        return requestDirector;
    }

    public OkHttpClient getHttpClient() {
        return requestDirector.getClient();
    }

    public void download(List<String> urls) {
        for (String url : urls) {
            extract(url, true);
        }
    }

    /** @deprecated use {@link #extract(String, boolean)} */
    public VideoInfo extractInfo(String url) {
        ExtractorResult result = extract(url, false);
        return result.isVideo() ? result.getVideo() : null;
    }

    /** @deprecated use {@link #extract(String, boolean)} */
    public VideoInfo extractInfo(String url, boolean download) {
        ExtractorResult result = extract(url, download);
        return result.isVideo() ? result.getVideo() : null;
    }

    public ExtractorResult extract(String url, boolean download) {
        InfoExtractor ie = registry.findSuitable(url);
        if (ie == null) {
            throw new ExtractorException(
                    "No suitable extractor found for URL: " + url
                            + ". Try enabling external yt-dlp via options.setExternalYtDlpEnabled(true).",
                    true);
        }

        if (!options.isQuiet()) {
            toScreen("[" + ie.getIeName() + "] Extracting URL: " + url);
        }

        ExtractorResult result = ie.extract(url);
        if (result == null) {
            throw new ExtractorException("Extractor returned no information");
        }

        printJson(result);

        if (download && !options.isSimulate() && !options.isSkipDownload()) {
            processResult(result);
        }

        return result;
    }

    private void printJson(ExtractorResult result) {
        if (!options.isPrintJson()) {
            return;
        }
        try {
            Object payload = result.isPlaylist() ? result.getPlaylist() : result.getVideo();
            System.out.println(jsonMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new YtDlpException("Failed to serialize JSON", e);
        }
    }

    private void processResult(ExtractorResult result) {
        if (result.isPlaylist()) {
            processPlaylist(result.getPlaylist());
        } else if (result.isVideo()) {
            processVideo(result.getVideo());
        }
    }

    private void processPlaylist(PlaylistInfo playlist) {
        List<VideoInfo> entries = playlist.getEntries();
        if (!options.isQuiet()) {
            toScreen("[download] Downloading playlist " + playlist.getTitle() + " - " + entries.size() + " videos");
        }
        for (VideoInfo entry : entries) {
            String entryUrl = entry.getUrl() != null ? entry.getUrl() : entry.getWebpageUrl();
            if (entryUrl == null) {
                reportWarning("Skipping playlist entry without URL: " + entry.getId());
                continue;
            }
            if (!options.isQuiet()) {
                toScreen("[download] Downloading item " + entry.getPlaylistIndex() + " of " + entries.size());
            }
            ExtractorResult entryResult = extract(entryUrl, false);
            if (entryResult.isVideo()) {
                processVideo(entryResult.getVideo());
            }
        }
    }

    private void processVideo(VideoInfo info) {
        if (isExternalExtracted(info)) {
            String downloadUrl = info.getWebpageUrl() != null ? info.getWebpageUrl() : info.getUrl();
            if (downloadUrl == null) {
                throw new YtDlpException("No URL available for external yt-dlp download");
            }
            getExternalBridge().download(downloadUrl);
            return;
        }

        Format selected = formatSelector.select(info, options.getFormat());
        if (selected == null) {
            throw new YtDlpException("Could not select a format to download");
        }

        String filename = UrlUtils.expandOutputTemplate(
                options.getOutputTemplate(),
                info.getId(),
                info.getTitle(),
                selected.getExt());

        Path destination = Path.of(options.getDownloadPath(), filename);
        downloaderFactory.download(info, selected, destination);
    }

    private boolean isExternalExtracted(VideoInfo info) {
        return "ExternalYtDlp".equals(info.getExtractorKey());
    }

    private YtDlpBridge getExternalBridge() {
        if (externalBridge == null) {
            externalBridge = new YtDlpBridge(options);
        }
        return externalBridge;
    }

    public void toScreen(String message) {
        if (!options.isQuiet()) {
            System.out.println(message);
        }
    }

    public void reportWarning(String message) {
        if (!options.isNoWarnings()) {
            System.err.println("WARNING: " + message);
        }
    }

    @Override
    public void close() {
        requestDirector.close();
    }
}
