package com.ytdlp.downloader;

import com.ytdlp.YoutubeDL;
import com.ytdlp.model.Format;
import com.ytdlp.model.Fragment;
import com.ytdlp.networking.Request;
import com.ytdlp.util.M3u8Parser;

import java.util.List;
import java.util.Map;

/**
 * HLS (m3u8) native downloader. Ported from yt-dlp's HlsFD.
 */
public class HlsDownloader extends FragmentDownloader {

    public HlsDownloader(YoutubeDL ydl) {
        super(ydl);
    }

    @Override
    protected List<Fragment> resolveFragments(Format format) {
        if (format.getFragments() != null && !format.getFragments().isEmpty()) {
            return format.getFragments();
        }
        String manifestUrl = format.getManifestUrl() != null ? format.getManifestUrl() : format.getUrl();
        Map<String, String> headers = format.getHttpHeaders();
        String manifest = ydl.getRequestDirector().downloadString(new Request(manifestUrl, "GET", null, headers));
        return M3u8Parser.parseSegments(manifest, manifestUrl);
    }
}
