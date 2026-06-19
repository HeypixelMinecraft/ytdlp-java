package com.ytdlp.extractor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractorRegistryTest {

    @Test
    void loadsManifestAndDedicatedExtractors() {
        ExtractorRegistry registry = new ExtractorRegistry();
        assertTrue(registry.size() >= 10);
        assertTrue(registry.findSuitable("https://www.youtube.com/watch?v=BaW_jenozKc") != null);
        assertTrue(registry.findSuitable("https://vimeo.com/123456789") != null);
        assertTrue(registry.findSuitable("https://www.xvideos.com/video12345/title") != null);
        assertTrue(registry.findSuitable("https://example.com/article/video-page") != null);
        assertTrue(registry.findSuitable("https://media.w3.org/2010/05/sintel/trailer.mp4") != null);
    }

    @Test
    void externalYtDlpDisabledByDefault() {
        ExtractorRegistry registry = new ExtractorRegistry(false);
        assertFalse(registry.getExtractor("ExternalYtDlp") != null
                && registry.getExtractors().stream().anyMatch(ie -> "ExternalYtDlp".equals(ie.getIeKey())));
    }
}
