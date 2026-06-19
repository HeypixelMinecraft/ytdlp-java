package com.ytdlp.extractor.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.PlaylistInfo;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.JsonUtils;
import com.ytdlp.util.YoutubeUrlUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YouTube playlist/tab extractor. Ported from yt-dlp's YoutubeTabIE / YoutubePlaylistIE.
 */
public class YoutubePlaylistExtractor extends InfoExtractor {
    private static final String VALID_URL =
            "(?i)(?:https?://)?(?:www\\.)?youtube\\.com/(?:playlist\\?|watch\\?.*\\blist=)([a-zA-Z0-9_-]+)";
    private static final String BROWSE_API = "https://www.youtube.com/youtubei/v1/browse?prettyPrint=false";

    public YoutubePlaylistExtractor() {
        super("youtube:playlist", "YoutubePlaylist", VALID_URL);
    }

    @Override
    public boolean suitable(String url) {
        if (!super.suitable(url)) {
            return false;
        }
        String playlistId = YoutubeUrlUtils.extractPlaylistId(url);
        if (playlistId == null) {
            return false;
        }
        // watch?v=...&list=... — respect --no-playlist (handled in YoutubeDL routing too)
        if (YoutubeUrlUtils.extractVideoId(url) != null && ydl != null && ydl.getOptions().isNoPlaylist()) {
            return false;
        }
        return true;
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        String playlistId = YoutubeUrlUtils.extractPlaylistId(url);
        if (playlistId == null) {
            throw new ExtractorException("Could not extract playlist ID from URL: " + url);
        }

        String webpageUrl = YoutubeUrlUtils.buildPlaylistUrl(playlistId);
        PlaylistInfo playlist = new PlaylistInfo();
        playlist.setId(playlistId);
        playlist.setWebpageUrl(webpageUrl);

        String webpage = downloadWebpage(webpageUrl, playlistId);
        JsonNode initialData = parseInitialData(webpage);
        if (initialData != null) {
            fillPlaylistMetadata(playlist, initialData);
            collectEntriesFromInitialData(playlist, initialData);
        }

        // Fallback / pagination via InnerTube browse API
        if (playlist.getEntries().isEmpty()) {
            fetchEntriesViaBrowseApi(playlist, playlistId);
        } else {
            fetchContinuationEntries(playlist, initialData);
        }

        if (playlist.getEntries().isEmpty()) {
            throw new ExtractorException("Playlist " + playlistId + " has no entries", true);
        }

        playlist.setPlaylistCount(playlist.getEntries().size());
        return ExtractorResult.playlist(playlist);
    }

    private JsonNode parseInitialData(String webpage) {
        String json = JsonUtils.extractJsonAfterMarker(webpage, "ytInitialData");
        return json != null ? JsonUtils.parse(json) : null;
    }

    private void fillPlaylistMetadata(PlaylistInfo playlist, JsonNode data) {
        for (JsonNode header : JsonUtils.traverse(data, "header", "playlistHeaderRenderer")) {
            playlist.setTitle(JsonUtils.getTextFlexible(header.get("title")));
            JsonNode desc = header.get("descriptionText");
            if (desc != null) {
                playlist.setDescription(JsonUtils.getTextFlexible(desc));
            }
            JsonNode owner = header.get("ownerText");
            if (owner != null) {
                playlist.setUploader(JsonUtils.getTextFlexible(owner));
            }
            JsonNode thumbs = header.get("playlistHeaderBanner");
            if (thumbs != null) {
                JsonNode renderer = thumbs.get("heroPlaylistThumbnailRenderer");
                if (renderer != null) {
                    JsonNode thumbList = renderer.path("thumbnail").path("thumbnails");
                    if (thumbList.isArray() && !thumbList.isEmpty()) {
                        playlist.setThumbnail(thumbList.get(thumbList.size() - 1).path("url").asText(null));
                    }
                }
            }
            return;
        }
        for (JsonNode micro : JsonUtils.traverse(data, "metadata", "playlistMetadataRenderer")) {
            if (playlist.getTitle() == null) {
                playlist.setTitle(JsonUtils.getTextFlexible(micro.get("title")));
            }
            if (playlist.getDescription() == null) {
                playlist.setDescription(JsonUtils.getTextFlexible(micro.get("description")));
            }
        }
    }

    private void collectEntriesFromInitialData(PlaylistInfo playlist, JsonNode data) {
        for (JsonNode renderer : JsonUtils.traverse(data, "playlistVideoRenderer")) {
            addPlaylistEntry(playlist, renderer);
        }
        for (JsonNode renderer : JsonUtils.traverse(data, "playlistPanelVideoRenderer")) {
            addPanelEntry(playlist, renderer);
        }
    }

    private void addPlaylistEntry(PlaylistInfo playlist, JsonNode renderer) {
        String videoId = JsonUtils.getText(renderer, "videoId");
        if (videoId == null || videoId.isBlank()) {
            return;
        }
        if (playlist.getEntries().stream().anyMatch(e -> videoId.equals(e.getId()))) {
            return;
        }
        VideoInfo entry = new VideoInfo();
        entry.setType(VideoInfo.TYPE_URL);
        entry.setId(videoId);
        entry.setTitle(JsonUtils.getTextFlexible(renderer.get("title")));
        entry.setUrl(YoutubeUrlUtils.buildWatchUrl(videoId));
        entry.setWebpageUrl(entry.getUrl());
        entry.setPlaylistIndex(playlist.getEntries().size() + 1);

        JsonNode thumbs = renderer.get("thumbnail");
        if (thumbs != null) {
            JsonNode thumbList = thumbs.get("thumbnails");
            if (thumbList != null && thumbList.isArray() && !thumbList.isEmpty()) {
                entry.setThumbnail(thumbList.get(thumbList.size() - 1).path("url").asText(null));
            }
        }
        JsonNode duration = renderer.get("lengthText");
        if (duration != null) {
            entry.setDuration(parseDurationText(JsonUtils.getTextFlexible(duration)));
        }
        playlist.getEntries().add(entry);
    }

    private void addPanelEntry(PlaylistInfo playlist, JsonNode renderer) {
        String videoId = JsonUtils.getText(renderer, "videoId");
        if (videoId == null) {
            return;
        }
        JsonNode nested = renderer.get("title");
        VideoInfo entry = new VideoInfo();
        entry.setType(VideoInfo.TYPE_URL);
        entry.setId(videoId);
        entry.setTitle(JsonUtils.getTextFlexible(nested));
        entry.setUrl(YoutubeUrlUtils.buildWatchUrl(videoId));
        entry.setWebpageUrl(entry.getUrl());
        entry.setPlaylistIndex(playlist.getEntries().size() + 1);
        playlist.getEntries().add(entry);
    }

    private void fetchEntriesViaBrowseApi(PlaylistInfo playlist, String playlistId) {
        JsonNode response = callBrowseApi("VL" + playlistId, null);
        if (response == null) {
            return;
        }
        collectEntriesFromBrowseResponse(playlist, response);
    }

    private void fetchContinuationEntries(PlaylistInfo playlist, JsonNode initialData) {
        if (initialData == null) {
            return;
        }
        String continuation = findContinuation(initialData);
        while (continuation != null && !continuation.isBlank()) {
            JsonNode response = callBrowseApi(null, continuation);
            if (response == null) {
                break;
            }
            int before = playlist.getEntries().size();
            collectEntriesFromBrowseResponse(playlist, response);
            if (playlist.getEntries().size() == before) {
                break;
            }
            continuation = findContinuation(response);
        }
    }

    private void collectEntriesFromBrowseResponse(PlaylistInfo playlist, JsonNode response) {
        for (JsonNode renderer : JsonUtils.traverse(response, "playlistVideoRenderer")) {
            addPlaylistEntry(playlist, renderer);
        }
        for (JsonNode item : JsonUtils.traverse(response, "itemSectionRenderer", "contents", "...",
                "playlistVideoListRenderer", "contents", "...", "playlistVideoRenderer")) {
            addPlaylistEntry(playlist, item);
        }
    }

    private String findContinuation(JsonNode data) {
        for (JsonNode cont : JsonUtils.traverse(data, "continuationEndpoint", "continuationCommand", "token")) {
            if (cont.isTextual()) {
                return cont.asText();
            }
        }
        for (JsonNode cont : JsonUtils.traverse(data, "nextContinuationData", "continuation")) {
            if (cont.isTextual()) {
                return cont.asText();
            }
        }
        return null;
    }

    private JsonNode callBrowseApi(String browseId, String continuation) {
        try {
            Map<String, Object> client = new LinkedHashMap<>();
            client.put("clientName", "WEB");
            client.put("clientVersion", "2.20260114.08.00");
            client.put("hl", "en");

            Map<String, Object> body = new HashMap<>();
            body.put("context", Map.of("client", client));
            if (browseId != null) {
                body.put("browseId", browseId);
            }
            if (continuation != null) {
                body.put("continuation", continuation);
            }

            String jsonBody = JsonUtils.mapper().writeValueAsString(body);
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("X-YouTube-Client-Name", "1");
            headers.put("X-YouTube-Client-Version", "2.20260114.08.00");
            headers.put("Origin", "https://www.youtube.com");

            String response = downloadJson(BROWSE_API, jsonBody.getBytes(StandardCharsets.UTF_8), headers);
            return JsonUtils.parse(response);
        } catch (Exception e) {
            ydl.reportWarning("Browse API failed: " + e.getMessage());
            return null;
        }
    }

    private Long parseDurationText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] parts = text.trim().split(":");
        try {
            long seconds = 0;
            for (String part : parts) {
                seconds = seconds * 60 + Long.parseLong(part.trim());
            }
            return seconds;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
