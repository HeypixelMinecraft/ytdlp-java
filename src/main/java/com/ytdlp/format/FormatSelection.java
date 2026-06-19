package com.ytdlp.format;

import com.ytdlp.model.Format;

/**
 * Result of format selection; may require merging multiple streams.
 */
public class FormatSelection {
    private Format singleFormat;
    private Format videoFormat;
    private Format audioFormat;
    private boolean mergeRequired;

    public static FormatSelection single(Format format) {
        FormatSelection s = new FormatSelection();
        s.singleFormat = format;
        s.mergeRequired = false;
        return s;
    }

    public static FormatSelection merge(Format video, Format audio) {
        FormatSelection s = new FormatSelection();
        s.videoFormat = video;
        s.audioFormat = audio;
        s.mergeRequired = true;
        return s;
    }

    public Format getSingleFormat() {
        return singleFormat;
    }

    public Format getVideoFormat() {
        return videoFormat;
    }

    public Format getAudioFormat() {
        return audioFormat;
    }

    public boolean isMergeRequired() {
        return mergeRequired;
    }
}
