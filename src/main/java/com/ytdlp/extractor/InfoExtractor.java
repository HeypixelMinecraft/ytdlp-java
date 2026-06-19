package com.ytdlp.extractor;

import com.ytdlp.YoutubeDL;
import com.ytdlp.model.ExtractorResult;

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

    public ExtractorResult extract(String url) {
        ExtractorResult result = realExtract(url);
        if (result == null) {
            return null;
        }
        if (result.isVideo() && result.getVideo() != null) {
            result.getVideo().setExtractor(ieName);
            result.getVideo().setExtractorKey(ieKey);
        }
        if (result.isPlaylist() && result.getPlaylist() != null) {
            result.getPlaylist().setExtractor(ieName);
            result.getPlaylist().setExtractorKey(ieKey);
        }
        return result;
    }

    protected abstract ExtractorResult realExtract(String url);

    protected String downloadWebpage(String url, String itemId) {
        return ydl.getRequestDirector().downloadString(new com.ytdlp.networking.Request(url));
    }

    protected String downloadJson(String url, byte[] data, java.util.Map<String, String> headers) {
        com.ytdlp.networking.Request request = new com.ytdlp.networking.Request(
                url, "POST", data, headers);
        return ydl.getRequestDirector().downloadString(request);
    }
}
