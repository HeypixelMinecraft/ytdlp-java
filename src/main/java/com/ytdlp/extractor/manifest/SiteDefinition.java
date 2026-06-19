package com.ytdlp.extractor.manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteDefinition {
    private String ieKey;
    private String ieName;
    private String urlRegex;
    private List<String> mediaRegexes = new ArrayList<>();
    private transient Pattern compiledUrl;

    public String getIeKey() {
        return ieKey;
    }

    public void setIeKey(String ieKey) {
        this.ieKey = ieKey;
    }

    public String getIeName() {
        return ieName;
    }

    public void setIeName(String ieName) {
        this.ieName = ieName;
    }

    public String getUrlRegex() {
        return urlRegex;
    }

    public void setUrlRegex(String urlRegex) {
        this.urlRegex = urlRegex;
        this.compiledUrl = null;
    }

    public List<String> getMediaRegexes() {
        return mediaRegexes;
    }

    public void setMediaRegexes(List<String> mediaRegexes) {
        this.mediaRegexes = mediaRegexes != null ? mediaRegexes : new ArrayList<>();
    }

    public boolean matches(String url) {
        if (compiledUrl == null) {
            compiledUrl = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        }
        return compiledUrl.matcher(url).find();
    }
}
