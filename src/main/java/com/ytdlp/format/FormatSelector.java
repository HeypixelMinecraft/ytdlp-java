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

    public FormatSelection selectFormats(VideoInfo info, String formatSpec) {
        List<Format> formats = info.getFormats();
        if (formats == null || formats.isEmpty()) {
            if (info.getUrl() != null) {
                Format direct = new Format();
                direct.setUrl(info.getUrl());
                direct.setExt(info.getExt());
                return FormatSelection.single(direct);
            }
            return null;
        }

        String spec = formatSpec != null ? formatSpec.trim() : "best";

        if (spec.contains("+")) {
            String[] parts = spec.split("\\+", 2);
            Format video = resolveSingle(info, parts[0].trim(), true);
            Format audio = resolveSingle(info, parts[1].trim(), false);
            return FormatSelection.merge(video, audio);
        }

        return FormatSelection.single(resolveSingle(info, spec, null));
    }

    /** @deprecated use {@link #selectFormats(VideoInfo, String)} */
    public Format select(VideoInfo info, String formatSpec) {
        FormatSelection sel = selectFormats(info, formatSpec);
        return sel != null ? sel.getSingleFormat() : null;
    }

    private Format resolveSingle(VideoInfo info, String spec, Boolean videoOnly) {
        List<Format> formats = info.getFormats();
        String lower = spec.toLowerCase();

        if (spec.matches("\\d+")) {
            return formats.stream()
                    .filter(f -> spec.equals(f.getFormatId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Format " + spec + " not available"));
        }

        if (Boolean.TRUE.equals(videoOnly) || "bestvideo".equals(lower) || lower.startsWith("bv")) {
            return formats.stream()
                    .filter(f -> !f.isHasDrm())
                    .filter(Format::isVideoOnly)
                    .max(Comparator.comparingInt(this::formatScore))
                    .orElseGet(() -> formats.stream().filter(Format::hasVideo).max(Comparator.comparingInt(this::formatScore)).orElse(formats.get(0)));
        }
        if (Boolean.FALSE.equals(videoOnly) || "bestaudio".equals(lower) || lower.startsWith("ba")) {
            return formats.stream()
                    .filter(f -> !f.isHasDrm())
                    .filter(Format::isAudioOnly)
                    .max(Comparator.comparingInt(f -> f.getTbr() != null ? f.getTbr() : 0))
                    .orElseGet(() -> formats.stream().filter(Format::hasAudio).max(Comparator.comparingInt(f -> f.getTbr() != null ? f.getTbr() : 0)).orElse(formats.get(0)));
        }

        return switch (lower) {
            case "worst" -> formats.stream()
                    .min(Comparator.comparingInt(this::formatScore))
                    .orElse(formats.get(0));
            default -> selectBest(formats);
        };
    }

    private Format selectBest(List<Format> formats) {
        List<Format> usable = formats.stream().filter(f -> !f.isHasDrm()).collect(Collectors.toList());
        if (usable.isEmpty()) {
            usable = formats;
        }
        List<Format> combined = usable.stream()
                .filter(f -> f.hasVideo() && f.hasAudio())
                .collect(Collectors.toList());

        if (!combined.isEmpty()) {
            return combined.stream()
                    .max(Comparator.comparingInt(this::formatScore))
                    .orElse(combined.get(0));
        }

        return usable.stream()
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
        if ("https".equals(f.getProtocol()) || "http".equals(f.getProtocol())) {
            score += 500;
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
