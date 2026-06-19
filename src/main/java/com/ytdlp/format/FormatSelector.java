package com.ytdlp.format;

import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Format selection logic ported from yt-dlp's format selector.
 */
public class FormatSelector {

    public Format select(VideoInfo info, String formatSpec) {
        List<Format> formats = info.getFormats();
        if (formats == null || formats.isEmpty()) {
            if (info.getUrl() != null) {
                Format direct = new Format();
                direct.setUrl(info.getUrl());
                direct.setExt(info.getExt());
                return direct;
            }
            return null;
        }

        String spec = formatSpec != null ? formatSpec.trim().toLowerCase() : "best";

        // Specific format ID
        if (spec.matches("\\d+")) {
            return formats.stream()
                    .filter(f -> spec.equals(f.getFormatId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Format " + spec + " not available"));
        }

        return switch (spec) {
            case "worst" -> formats.stream()
                    .min(Comparator.comparingInt(this::formatScore))
                    .orElse(formats.get(0));
            case "bestaudio" -> formats.stream()
                    .filter(Format::isAudioOnly)
                    .max(Comparator.comparingInt(f -> f.getTbr() != null ? f.getTbr() : 0))
                    .orElseGet(() -> selectBest(formats));
            case "bestvideo" -> formats.stream()
                    .filter(Format::isVideoOnly)
                    .max(Comparator.comparingInt(this::resolutionScore))
                    .orElseGet(() -> selectBest(formats));
            default -> selectBest(formats);
        };
    }

    private Format selectBest(List<Format> formats) {
        // Prefer combined formats (video+audio) with highest resolution
        List<Format> combined = formats.stream()
                .filter(f -> f.hasVideo() && f.hasAudio())
                .collect(Collectors.toList());

        if (!combined.isEmpty()) {
            return combined.stream()
                    .max(Comparator.comparingInt(this::formatScore))
                    .orElse(combined.get(0));
        }

        return formats.stream()
                .max(Comparator.comparingInt(this::formatScore))
                .orElse(formats.get(0));
    }

    private int formatScore(Format f) {
        int score = resolutionScore(f);
        if (f.getTbr() != null) {
            score += f.getTbr();
        }
        if (f.hasVideo() && f.hasAudio()) {
            score += 10000;
        }
        return score;
    }

    private int resolutionScore(Format f) {
        if (f.getHeight() != null) {
            return f.getHeight();
        }
        if (f.getWidth() != null) {
            return f.getWidth();
        }
        return 0;
    }
}
