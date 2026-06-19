package com.ytdlp.youtube.pot;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.networking.Request;
import com.ytdlp.networking.RequestDirector;
import com.ytdlp.util.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches PO token via InnerTube attestation-style API when visitor data is available.
 * Falls back to configured token from options.
 */
public class InnertubePoTokenProvider implements PoTokenProvider {
    private static final String PO_TOKEN_API = "https://www.youtube.com/youtubei/v1/att/get?prettyPrint=false";

    private final YoutubeDLOptions options;
    private final RequestDirector director;
    private final ConfiguredPoTokenProvider fallback;

    public InnertubePoTokenProvider(YoutubeDLOptions options, RequestDirector director) {
        this.options = options;
        this.director = director;
        this.fallback = new ConfiguredPoTokenProvider(options);
    }

    @Override
    public String getPlayerPoToken(String videoId, String visitorData, String playerUrl) {
        String configured = fallback.getPlayerPoToken(videoId, visitorData, playerUrl);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return fetchPoToken(videoId, visitorData, "PLAYER");
    }

    @Override
    public String getGvsPoToken(String videoId, String visitorData, String streamUrl) {
        String configured = fallback.getGvsPoToken(videoId, visitorData, streamUrl);
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return fetchPoToken(videoId, visitorData, "GVS");
    }

    private String fetchPoToken(String videoId, String visitorData, String context) {
        if (visitorData == null || visitorData.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            Map<String, Object> innerContext = new HashMap<>();
            Map<String, Object> client = new HashMap<>();
            client.put("clientName", "WEB");
            client.put("clientVersion", "2.20260114.08.00");
            client.put("hl", "en");
            innerContext.put("client", client);
            body.put("context", innerContext);
            body.put("engagementType", context);
            body.put("contentBinding", videoId);

            String json = JsonUtils.mapper().writeValueAsString(body);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-Goog-Visitor-Id", visitorData);
            headers.put("X-YouTube-Client-Name", "1");
            headers.put("X-YouTube-Client-Version", "2.20260114.08.00");
            headers.put("Origin", "https://www.youtube.com");

            String response = director.downloadString(new Request(
                    PO_TOKEN_API, "POST", json.getBytes(StandardCharsets.UTF_8), headers));
            JsonNode root = JsonUtils.parse(response);
            String token = JsonUtils.getText(root, "poToken");
            if (token == null) {
                token = JsonUtils.getText(root, "botguardResponse", "poToken");
            }
            return token;
        } catch (Exception e) {
            return null;
        }
    }
}
