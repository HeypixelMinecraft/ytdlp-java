package com.ytdlp.downloader;

import com.ytdlp.YoutubeDL;
import com.ytdlp.model.Format;
import com.ytdlp.model.Fragment;
import com.ytdlp.networking.Request;
import com.ytdlp.util.DashMpdParser;

import java.util.List;

/**
 * DASH (MPD) segment downloader. Ported from yt-dlp's DashSegmentsFD.
 */
public class DashDownloader extends FragmentDownloader {

    public DashDownloader(YoutubeDL ydl) {
        super(ydl);
    }

    @Override
    protected List<Fragment> resolveFragments(Format format) {
        if (format.getFragments() != null && !format.getFragments().isEmpty()) {
            return format.getFragments();
        }
        String manifestUrl = format.getManifestUrl() != null ? format.getManifestUrl() : format.getUrl();
        String mpd = ydl.getRequestDirector().downloadString(new Request(manifestUrl));
        return DashMpdParser.parseSegments(mpd, manifestUrl);
    }
}
