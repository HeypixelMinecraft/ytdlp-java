package com.ytdlp.extractor.manifest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ytdlp.util.JsonUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class SiteManifest {
    private static final String RESOURCE = "/extractors/sites.json";
    private static List<SiteDefinition> cached;

    private SiteManifest() {
    }

    public static List<SiteDefinition> load() {
        if (cached != null) {
            return cached;
        }
        try (InputStream in = SiteManifest.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                cached = List.of();
                return cached;
            }
            cached = JsonUtils.mapper().readValue(in, new TypeReference<List<SiteDefinition>>() {});
            return cached;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load site manifest: " + RESOURCE, e);
        }
    }

    public static SiteDefinition findForUrl(String url) {
        for (SiteDefinition site : load()) {
            if (site.matches(url)) {
                return site;
            }
        }
        return null;
    }

    public static List<SiteDefinition> all() {
        return new ArrayList<>(load());
    }
}
