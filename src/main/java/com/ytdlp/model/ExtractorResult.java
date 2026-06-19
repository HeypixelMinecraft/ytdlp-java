package com.ytdlp.model;

/**
 * Unified result from an {@link com.ytdlp.extractor.InfoExtractor}.
 * Either a single {@link VideoInfo} or a {@link PlaylistInfo}.
 */
public class ExtractorResult {
    private VideoInfo video;
    private PlaylistInfo playlist;

    public static ExtractorResult video(VideoInfo video) {
        ExtractorResult r = new ExtractorResult();
        r.video = video;
        return r;
    }

    public static ExtractorResult playlist(PlaylistInfo playlist) {
        ExtractorResult r = new ExtractorResult();
        r.playlist = playlist;
        return r;
    }

    public boolean isPlaylist() {
        return playlist != null;
    }

    public boolean isVideo() {
        return video != null;
    }

    public VideoInfo getVideo() {
        return video;
    }

    public PlaylistInfo getPlaylist() {
        return playlist;
    }
}
