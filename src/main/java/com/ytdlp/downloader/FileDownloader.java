package com.ytdlp.downloader;

import com.ytdlp.YoutubeDL;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;

import java.nio.file.Path;

public interface FileDownloader {
    void download(VideoInfo info, Format format, Path destination);
}
