package com.ytdlp.bridge;

import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.exception.YtDlpException;
import com.ytdlp.model.ExtractorResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Invokes the official yt-dlp binary to support 1000+ sites via subprocess bridge.
 * Native Java extractors are tried first; this is the universal fallback.
 */
public class YtDlpBridge {
    private final YoutubeDLOptions options;
    private final String executable;
    private final Boolean available;

    public YtDlpBridge(YoutubeDLOptions options) {
        this.options = options;
        this.executable = resolveExecutable(options.getExternalYtDlpPath());
        this.available = executable != null ? checkAvailable(executable) : false;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getExecutable() {
        return executable;
    }

    public ExtractorResult extract(String url) {
        List<String> command = baseCommand();
        command.add("--no-download");
        command.add("-J");
        command.add(url);

        String json = run(command, options.getExternalYtDlpTimeout());
        try {
            return YtDlpJsonConverter.convert(json);
        } catch (Exception e) {
            throw new ExtractorException("Failed to parse yt-dlp JSON: " + e.getMessage(), e);
        }
    }

    public void download(String url) {
        List<String> command = baseCommand();
        command.add("-f");
        command.add(options.getFormat());
        command.add("-o");
        command.add(Path.of(options.getDownloadPath(), options.getOutputTemplate()).toString());
        if (options.isNoPlaylist()) {
            command.add("--no-playlist");
        }
        command.add(url);
        run(command, options.getExternalYtDlpTimeout() * 10L);
    }

    private List<String> baseCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(executable);
        if (options.isQuiet()) {
            cmd.add("-q");
        }
        if (options.isNoWarnings()) {
            cmd.add("--no-warnings");
        }
        if (options.getCookieFile() != null && !options.getCookieFile().isBlank()) {
            cmd.add("--cookies");
            cmd.add(options.getCookieFile());
        }
        if (options.getProxy() != null && !options.getProxy().isBlank()) {
            cmd.add("--proxy");
            cmd.add(options.getProxy());
        }
        if (options.getUserAgent() != null && !options.getUserAgent().isBlank()) {
            cmd.add("--user-agent");
            cmd.add(options.getUserAgent());
        }
        return cmd;
    }

    private String run(List<String> command, long timeoutSeconds) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new YtDlpException("yt-dlp timed out after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0) {
                throw new ExtractorException(trimError(output.toString()), true);
            }
            return output.toString().trim();
        } catch (ExtractorException e) {
            throw e;
        } catch (YtDlpException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtractorException("Failed to run yt-dlp: " + e.getMessage(), e);
        }
    }

    private static String trimError(String output) {
        if (output.length() > 500) {
            return output.substring(0, 500) + "...";
        }
        return output.isBlank() ? "yt-dlp failed" : output.trim();
    }

    private static String resolveExecutable(String configured) {
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured);
            if (Files.isExecutable(path)) {
                return path.toString();
            }
            return configured;
        }
        for (String name : List.of("yt-dlp", "yt-dlp.exe")) {
            if (checkAvailable(name)) {
                return name;
            }
        }
        return null;
    }

    private static boolean checkAvailable(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
