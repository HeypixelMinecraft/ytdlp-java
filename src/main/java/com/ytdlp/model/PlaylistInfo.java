package com.ytdlp.model;

import java.util.ArrayList;
import java.util.List;

public class PlaylistInfo {
    private String id;
    private String title;
    private String description;
    private String uploader;
    private String channelId;
    private String webpageUrl;
    private String thumbnail;
    private Integer playlistCount;
    private String extractor;
    private String extractorKey;
    private List<VideoInfo> entries = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getWebpageUrl() {
        return webpageUrl;
    }

    public void setWebpageUrl(String webpageUrl) {
        this.webpageUrl = webpageUrl;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Integer getPlaylistCount() {
        return playlistCount;
    }

    public void setPlaylistCount(Integer playlistCount) {
        this.playlistCount = playlistCount;
    }

    public String getExtractor() {
        return extractor;
    }

    public void setExtractor(String extractor) {
        this.extractor = extractor;
    }

    public String getExtractorKey() {
        return extractorKey;
    }

    public void setExtractorKey(String extractorKey) {
        this.extractorKey = extractorKey;
    }

    public List<VideoInfo> getEntries() {
        return entries;
    }

    public void setEntries(List<VideoInfo> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }
}
