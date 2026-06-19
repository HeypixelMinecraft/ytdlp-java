package com.ytdlp.downloader;

import com.ytdlp.YoutubeDL;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;

import java.nio.file.Path;

/**
 * Selects protocol-specific downloader. Ported from yt-dlp downloader/__init__.py.
 */
public class DownloaderFactory {
    private final YoutubeDL ydl;
    private final HttpDownloader httpDownloader;
    private final HlsDownloader hlsDownloader;
    private final DashDownloader dashDownloader;

    public DownloaderFactory(YoutubeDL ydl) {
        this.ydl = ydl;
        this.httpDownloader = new HttpDownloader(ydl);
        this.hlsDownloader = new HlsDownloader(ydl);
        this.dashDownloader = new DashDownloader(ydl);
    }

    public FileDownloader getDownloader(Format format) {
        String protocol = format.getProtocol() != null ? format.getProtocol() : "https";
        return switch (protocol) {
            case "m3u8", "m3u8_native" -> hlsDownloader;
            case "http_dash_segments", "dash" -> dashDownloader;
            default -> httpDownloader;
        };
    }

    public void download(VideoInfo info, Format format, Path destination) {
        getDownloader(format).download(info, format, destination);
    }
}
