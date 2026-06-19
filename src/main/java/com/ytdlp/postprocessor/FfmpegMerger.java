package com.ytdlp.postprocessor;

import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.exception.DownloadException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg merge/remux post-processor. Ported from yt-dlp's FFmpegMergerPP / FFmpegVideoConvertorPP.
 */
public class FfmpegMerger {
    private final YoutubeDLOptions options;

    public FfmpegMerger(YoutubeDLOptions options) {
        this.options = options;
    }

    public boolean isAvailable() {
        return resolveFfmpeg() != null;
    }

    public void merge(Path videoFile, Path audioFile, Path output, String outputFormat) {
        String ffmpeg = resolveFfmpeg();
        if (ffmpeg == null) {
            throw new DownloadException("ffmpeg not found in PATH; install ffmpeg or set --ffmpeg-location");
        }
        if (!Files.exists(videoFile) || !Files.exists(audioFile)) {
            throw new DownloadException("Missing input files for merge");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpeg);
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(videoFile.toString());
        cmd.add("-i");
        cmd.add(audioFile.toString());
        cmd.add("-c");
        cmd.add("copy");
        if (outputFormat != null && !outputFormat.isBlank()) {
            cmd.add("-f");
            cmd.add(outputFormat);
        }
        cmd.add(output.toString());

        run(cmd);
    }

    public void remux(Path input, Path output) {
        String ffmpeg = resolveFfmpeg();
        if (ffmpeg == null) {
            throw new DownloadException("ffmpeg not found");
        }
        List<String> cmd = List.of(
                ffmpeg, "-y", "-i", input.toString(),
                "-c", "copy", output.toString()
        );
        run(cmd);
    }

    private void run(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!options.isQuiet()) {
                        System.err.println("[ffmpeg] " + line);
                    }
                }
            }
            if (!process.waitFor(30, TimeUnit.MINUTES) || process.exitValue() != 0) {
                process.destroyForcibly();
                throw new DownloadException("ffmpeg exited with code " + process.exitValue());
            }
        } catch (DownloadException e) {
            throw e;
        } catch (Exception e) {
            throw new DownloadException("ffmpeg failed: " + e.getMessage(), e);
        }
    }

    private String resolveFfmpeg() {
        if (options.getFfmpegLocation() != null && !options.getFfmpegLocation().isBlank()) {
            Path p = Path.of(options.getFfmpegLocation());
            if (Files.isExecutable(p)) {
                return p.toString();
            }
        }
        for (String name : List.of("ffmpeg", "ffmpeg.exe")) {
            try {
                Process p = new ProcessBuilder(name, "-version").redirectErrorStream(true).start();
                if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return name;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
