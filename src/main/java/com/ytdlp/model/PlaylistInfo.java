package com.ytdlp.model;

import java.util.ArrayList;
import java.util.List;

public class PlaylistInfo {
    private String id;
    private String title;
    private String description;
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

    public List<VideoInfo> getEntries() {
        return entries;
    }

    public void setEntries(List<VideoInfo> entries) {
        this.entries = entries;
    }
}
