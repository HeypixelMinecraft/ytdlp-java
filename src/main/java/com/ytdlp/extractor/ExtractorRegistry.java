package com.ytdlp.extractor;

import com.ytdlp.extractor.generic.GenericExtractor;
import com.ytdlp.extractor.youtube.YoutubeExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExtractorRegistry {
    private final Map<String, InfoExtractor> extractors = new LinkedHashMap<>();

    public ExtractorRegistry() {
        register(new YoutubeExtractor());
        register(new GenericExtractor());
    }

    public void register(InfoExtractor extractor) {
        extractors.put(extractor.getIeKey(), extractor);
    }

    public List<InfoExtractor> getExtractors() {
        return new ArrayList<>(extractors.values());
    }

    public InfoExtractor getExtractor(String key) {
        return extractors.get(key);
    }

    public InfoExtractor findSuitable(String url) {
        for (InfoExtractor ie : extractors.values()) {
            if (ie.suitable(url)) {
                return ie;
            }
        }
        return null;
    }
}
