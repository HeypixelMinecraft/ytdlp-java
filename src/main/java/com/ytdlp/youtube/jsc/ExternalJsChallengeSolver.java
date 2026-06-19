package com.ytdlp.youtube.jsc;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.util.JsonUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Invokes an external JS runtime (deno/node) with yt-dlp-ejs solver script.
 * Mirrors yt-dlp's External JS challenge providers.
 */
public class ExternalJsChallengeSolver implements JsChallengeSolver {
    private final YoutubeDLOptions options;
    private final String runtimeExecutable;
    private final Path solverScript;

    public ExternalJsChallengeSolver(YoutubeDLOptions options) {
        this.options = options;
        this.runtimeExecutable = resolveRuntime(options.getJsRuntime());
        this.solverScript = resolveSolverScript(options.getEjsScriptPath());
    }

    @Override
    public boolean isAvailable() {
        return runtimeExecutable != null && solverScript != null && Files.isRegularFile(solverScript);
    }

    @Override
    public Map<String, String> solveN(List<String> challenges, String playerUrl, String videoId) {
        return solve("n", challenges, playerUrl, videoId);
    }

    @Override
    public Map<String, String> solveSig(List<String> challenges, String playerUrl, String videoId) {
        return solve("sig", challenges, playerUrl, videoId);
    }

    private Map<String, String> solve(String type, List<String> challenges, String playerUrl, String videoId) {
        if (!isAvailable() || challenges == null || challenges.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("type", type);
            request.put("videoId", videoId);
            request.put("playerUrl", playerUrl);
            request.put("challenges", challenges);

            String json = JsonUtils.mapper().writeValueAsString(request);

            ProcessBuilder pb = new ProcessBuilder(
                    runtimeExecutable, "run",
                    solverScript.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            process.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            boolean finished = process.waitFor(options.getJsSolverTimeout(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Collections.emptyMap();
            }
            if (process.exitValue() != 0) {
                return Collections.emptyMap();
            }

            JsonNode root = JsonUtils.parse(output.toString());
            JsonNode results = root.get("results");
            if (results == null || !results.isObject()) {
                return Collections.emptyMap();
            }
            Map<String, String> map = new HashMap<>();
            results.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asText()));
            return map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String resolveRuntime(String configured) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        for (String candidate : List.of("deno", "node", "nodejs")) {
            if (isOnPath(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path resolveSolverScript(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        // Default: bundled bridge script in resources copied to temp or referenced
        try {
            var url = ExternalJsChallengeSolver.class.getResource("/jsc/challenge-bridge.js");
            if (url != null) {
                return Path.of(url.toURI());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isOnPath(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            return p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
