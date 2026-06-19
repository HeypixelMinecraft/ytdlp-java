package com.ytdlp.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.PlaylistInfo;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts yt-dlp --dump-single-json (-J) output into library models.
 */
public final class YtDlpJsonConverter {

    private YtDlpJsonConverter() {
    }

    public static ExtractorResult convert(String json) {
        JsonNode root = JsonUtils.parse(json);
        String type = JsonUtils.getText(root, "_type");
        if ("playlist".equals(type) || "multi_video".equals(type)) {
            return ExtractorResult.playlist(convertPlaylist(root));
        }
        return ExtractorResult.video(convertVideo(root));
    }

    private static PlaylistInfo convertPlaylist(JsonNode root) {
        PlaylistInfo playlist = new PlaylistInfo();
        playlist.setId(JsonUtils.getText(root, "id"));
        playlist.setTitle(JsonUtils.getText(root, "title"));
        playlist.setDescription(JsonUtils.getText(root, "description"));
        playlist.setUploader(JsonUtils.getText(root, "uploader"));
        playlist.setChannelId(JsonUtils.getText(root, "channel_id"));
        playlist.setWebpageUrl(JsonUtils.getText(root, "webpage_url"));
        playlist.setThumbnail(JsonUtils.getText(root, "thumbnail"));

        JsonNode entries = root.get("entries");
        if (entries != null && entries.isArray()) {
            int index = 1;
            for (JsonNode entry : entries) {
                if (entry == null || entry.isNull()) {
                    continue;
                }
                VideoInfo video = convertEntry(entry);
                video.setPlaylistIndex(index++);
                playlist.getEntries().add(video);
            }
        }
        playlist.setPlaylistCount(playlist.getEntries().size());
        return playlist;
    }

    private static VideoInfo convertEntry(JsonNode entry) {
        String entryType = JsonUtils.getText(entry, "_type");
        if ("url".equals(entryType) || "url_transparent".equals(entryType)) {
            VideoInfo info = new VideoInfo();
            info.setType(VideoInfo.TYPE_URL);
            info.setId(JsonUtils.getText(entry, "id"));
            info.setTitle(JsonUtils.getText(entry, "title"));
            info.setUrl(JsonUtils.getText(entry, "url"));
            info.setWebpageUrl(info.getUrl());
            return info;
        }
        return convertVideo(entry);
    }

    private static VideoInfo convertVideo(JsonNode root) {
        VideoInfo info = new VideoInfo();
        info.setType(VideoInfo.TYPE_VIDEO);
        info.setId(JsonUtils.getText(root, "id"));
        info.setTitle(JsonUtils.getText(root, "title"));
        info.setDescription(JsonUtils.getText(root, "description"));
        info.setUrl(JsonUtils.getText(root, "url"));
        info.setExt(JsonUtils.getText(root, "ext"));
        info.setUploader(JsonUtils.getText(root, "uploader"));
        info.setChannel(JsonUtils.getText(root, "channel"));
        info.setChannelId(JsonUtils.getText(root, "channel_id"));
        info.setDuration(JsonUtils.getLong(root, "duration"));
        info.setViewCount(JsonUtils.getLong(root, "view_count"));
        info.setThumbnail(JsonUtils.getText(root, "thumbnail"));
        info.setWebpageUrl(JsonUtils.getText(root, "webpage_url"));
        info.setExtractor(JsonUtils.getText(root, "extractor"));
        info.setExtractorKey(JsonUtils.getText(root, "extractor_key"));

        JsonNode formats = root.get("formats");
        if (formats != null && formats.isArray()) {
            List<Format> list = new ArrayList<>();
            for (JsonNode f : formats) {
                Format format = convertFormat(f);
                if (format.getUrl() != null || format.getManifestUrl() != null) {
                    list.add(format);
                }
            }
            info.setFormats(list);
        }
        return info;
    }

    private static Format convertFormat(JsonNode f) {
        Format format = new Format();
        format.setFormatId(JsonUtils.getText(f, "format_id"));
        format.setFormatNote(JsonUtils.getText(f, "format_note"));
        format.setUrl(JsonUtils.getText(f, "url"));
        format.setExt(JsonUtils.getText(f, "ext"));
        format.setProtocol(JsonUtils.getText(f, "protocol"));
        format.setVcodec(JsonUtils.getText(f, "vcodec"));
        format.setAcodec(JsonUtils.getText(f, "acodec"));
        format.setWidth(JsonUtils.getInt(f, "width"));
        format.setHeight(JsonUtils.getInt(f, "height"));
        format.setFps(JsonUtils.getInt(f, "fps"));
        format.setTbr(JsonUtils.getInt(f, "tbr"));
        format.setFilesize(JsonUtils.getLong(f, "filesize"));
        format.setManifestUrl(JsonUtils.getText(f, "manifest_url"));
        if (format.getProtocol() == null) {
            format.setProtocol("https");
        }
        return format;
    }
}
