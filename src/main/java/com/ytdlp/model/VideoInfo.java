package com.ytdlp.model;

import java.util.ArrayList;
import java.util.List;

public class VideoInfo {
    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_PLAYLIST = "playlist";
    public static final String TYPE_URL = "url";

    private String type = TYPE_VIDEO;
    private String id;
    private String title;
    private String description;
    private String url;
    private String ext;
    private String uploader;
    private String channel;
    private String channelId;
    private Long duration;
    private Long viewCount;
    private String thumbnail;
    private List<Format> formats = new ArrayList<>();
    private String webpageUrl;
    private String extractor;
    private String extractorKey;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<Format> getFormats() {
        return formats;
    }

    public void setFormats(List<Format> formats) {
        this.formats = formats;
    }

    public String getWebpageUrl() {
        return webpageUrl;
    }

    public void setWebpageUrl(String webpageUrl) {
        this.webpageUrl = webpageUrl;
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
}
