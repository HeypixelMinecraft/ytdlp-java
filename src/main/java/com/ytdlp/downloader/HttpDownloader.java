package com.ytdlp.downloader;

import com.ytdlp.YoutubeDL;
import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.exception.DownloadException;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.networking.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP file downloader ported from yt-dlp's HttpFD.
 */
public class HttpDownloader implements FileDownloader {
    private final YoutubeDL ydl;
    private final YoutubeDLOptions options;

    public HttpDownloader(YoutubeDL ydl) {
        this.ydl = ydl;
        this.options = ydl.getOptions();
    }

    @Override
    public void download(VideoInfo info, Format format, Path destination) {
        String url = format.getUrl();
        if (url == null || url.isBlank()) {
            throw new DownloadException("No URL to download");
        }

        Path tempFile = Path.of(destination.toString() + ".part");
        long resumeLen = 0;
        if (options.isContinueDownload() && Files.exists(tempFile)) {
            try {
                resumeLen = Files.size(tempFile);
            } catch (IOException e) {
                resumeLen = 0;
            }
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept-Encoding", "identity");
        if (format.getHttpHeaders() != null) {
            headers.putAll(format.getHttpHeaders());
        }
        if (resumeLen > 0) {
            headers.put("Range", "bytes=" + resumeLen + "-");
            ydl.toScreen("[download] Resuming download at byte " + resumeLen);
        }

        Request request = new Request(url, "GET", null, headers);

        try (Response response = ydl.getRequestDirector().send(request)) {
            if (response.getStatusCode() >= 400) {
                throw new DownloadException("HTTP " + response.getStatusCode() + " while downloading");
            }

            long totalSize = parseContentLength(response.getHeader("Content-Length"));
            if (totalSize > 0 && resumeLen > 0) {
                totalSize += resumeLen;
            }

            StandardOpenOption[] openOptions = resumeLen > 0
                    ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}
                    : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};

            long downloaded;
            try (InputStream in = response.getBody();
                 OutputStream out = Files.newOutputStream(tempFile, openOptions)) {

                byte[] buffer = new byte[options.getBufferSize() * 1024];
                downloaded = resumeLen;
                int read;
                long startTime = System.currentTimeMillis();

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;
                    reportProgress(downloaded, totalSize, startTime);
                }
            }

            Files.move(tempFile, destination,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);

            ydl.toScreen("[download] 100% of " + formatSize(downloaded) + " saved to " + destination);
        } catch (IOException e) {
            throw new DownloadException("Download failed: " + e.getMessage(), e);
        }
    }

    private void reportProgress(long downloaded, long total, long startTime) {
        if (options.isQuiet()) {
            return;
        }
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        double speed = elapsed > 0 ? downloaded / elapsed : 0;
        if (total > 0) {
            double pct = downloaded * 100.0 / total;
            ydl.toScreen(String.format("[download] %5.1f%% of %s at %s/s",
                    pct, formatSize(total), formatSize((long) speed)));
        }
    }

    private long parseContentLength(String header) {
        if (header == null) {
            return -1;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + "B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1fKiB", bytes / 1024.0);
        }
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1fMiB", bytes / (1024.0 * 1024));
        }
        return String.format("%.1fGiB", bytes / (1024.0 * 1024 * 1024));
    }
}
