package com.ytdlp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Format {
    private String url;
    private String ext;
    private String formatId;
    private String formatNote;
    private String protocol = "https";
    private String vcodec;
    private String acodec;
    private Integer width;
    private Integer height;
    private Integer fps;
    private Integer tbr;
    private Long filesize;
    private Map<String, String> httpHeaders = new HashMap<>();
    private String manifestUrl;
    private List<Fragment> fragments = new ArrayList<>();
    private boolean hasDrm;

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

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getFormatNote() {
        return formatNote;
    }

    public void setFormatNote(String formatNote) {
        this.formatNote = formatNote;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVcodec() {
        return vcodec;
    }

    public void setVcodec(String vcodec) {
        this.vcodec = vcodec;
    }

    public String getAcodec() {
        return acodec;
    }

    public void setAcodec(String acodec) {
        this.acodec = acodec;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getFps() {
        return fps;
    }

    public void setFps(Integer fps) {
        this.fps = fps;
    }

    public Integer getTbr() {
        return tbr;
    }

    public void setTbr(Integer tbr) {
        this.tbr = tbr;
    }

    public Long getFilesize() {
        return filesize;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }

    public Map<String, String> getHttpHeaders() {
        return httpHeaders;
    }

    public void setHttpHeaders(Map<String, String> httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public boolean isVideoOnly() {
        return vcodec != null && !"none".equals(vcodec) && (acodec == null || "none".equals(acodec));
    }

    public boolean isAudioOnly() {
        return acodec != null && !"none".equals(acodec) && (vcodec == null || "none".equals(vcodec));
    }

    public boolean hasVideo() {
        return vcodec != null && !"none".equals(vcodec);
    }

    public boolean hasAudio() {
        return acodec != null && !"none".equals(acodec);
    }

    public String getManifestUrl() {
        return manifestUrl;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public List<Fragment> getFragments() {
        return fragments;
    }

    public void setFragments(List<Fragment> fragments) {
        this.fragments = fragments != null ? fragments : new ArrayList<>();
    }

    public boolean isHasDrm() {
        return hasDrm;
    }

    public void setHasDrm(boolean hasDrm) {
        this.hasDrm = hasDrm;
    }
}
