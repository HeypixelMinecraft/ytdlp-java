package com.ytdlp.extractor;

import com.ytdlp.YoutubeDL;
import com.ytdlp.model.VideoInfo;

import java.util.regex.Pattern;

public abstract class InfoExtractor {
    protected YoutubeDL ydl;
    protected final String ieName;
    protected final String ieKey;
    protected final Pattern validUrl;

    protected InfoExtractor(String ieName, String ieKey, String validUrlRegex) {
        this.ieName = ieName;
        this.ieKey = ieKey;
        this.validUrl = Pattern.compile(validUrlRegex, Pattern.CASE_INSENSITIVE);
    }

    public void setDownloader(YoutubeDL ydl) {
        this.ydl = ydl;
    }

    public String getIeName() {
        return ieName;
    }

    public String getIeKey() {
        return ieKey;
    }

    public boolean suitable(String url) {
        return validUrl.matcher(url).find();
    }

    public VideoInfo extract(String url) {
        VideoInfo info = realExtract(url);
        if (info != null) {
            info.setExtractor(ieName);
            info.setExtractorKey(ieKey);
        }
        return info;
    }

    protected abstract VideoInfo realExtract(String url);

    protected String downloadWebpage(String url, String videoId) {
        return ydl.getRequestDirector().downloadString(new com.ytdlp.networking.Request(url));
    }

    protected String downloadJson(String url, byte[] data, java.util.Map<String, String> headers) {
        com.ytdlp.networking.Request request = new com.ytdlp.networking.Request(
                url, "POST", data, headers);
        return ydl.getRequestDirector().downloadString(request);
    }
}
