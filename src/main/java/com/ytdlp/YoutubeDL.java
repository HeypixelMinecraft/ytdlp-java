package com.ytdlp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ytdlp.downloader.HttpDownloader;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.exception.YtDlpException;
import com.ytdlp.extractor.ExtractorRegistry;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.format.FormatSelector;
import com.ytdlp.model.Format;
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
    private final HttpDownloader httpDownloader;
    private final ObjectMapper jsonMapper;

    public YoutubeDL(YoutubeDLOptions options) {
        this(options, null);
    }

    /**
     * @param options    download/extract options
     * @param httpClient optional shared OkHttpClient; if null a client is created from options
     */
    public YoutubeDL(YoutubeDLOptions options, OkHttpClient httpClient) {
        this.options = options;
        this.registry = new ExtractorRegistry();
        this.requestDirector = new RequestDirector(options, httpClient);
        this.formatSelector = new FormatSelector();
        this.httpDownloader = new HttpDownloader(this);
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
            extractInfo(url, true);
        }
    }

    public VideoInfo extractInfo(String url) {
        return extractInfo(url, false);
    }

    public VideoInfo extractInfo(String url, boolean download) {
        InfoExtractor ie = registry.findSuitable(url);
        if (ie == null) {
            throw new ExtractorException("No suitable extractor found for URL: " + url);
        }

        if (!options.isQuiet()) {
            toScreen("[" + ie.getIeName() + "] Extracting URL: " + url);
        }

        VideoInfo info = ie.extract(url);
        if (info == null) {
            throw new ExtractorException("Extractor returned no information");
        }

        if (options.isPrintJson()) {
            try {
                System.out.println(jsonMapper.writeValueAsString(info));
            } catch (Exception e) {
                throw new YtDlpException("Failed to serialize JSON", e);
            }
        }

        if (download && !options.isSimulate() && !options.isSkipDownload()) {
            processInfo(info);
        }

        return info;
    }

    private void processInfo(VideoInfo info) {
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
        httpDownloader.download(info, selected, destination);
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
