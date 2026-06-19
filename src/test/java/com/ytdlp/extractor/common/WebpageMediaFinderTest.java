package com.ytdlp.extractor.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebpageMediaFinderTest {

    @Test
    void findsOpenGraphVideo() {
        String html = """
                <html><head>
                <meta property="og:title" content="Test Video"/>
                <meta property="og:video" content="https://cdn.example.com/video.mp4"/>
                </head><body></body></html>
                """;
        var info = WebpageMediaFinder.buildVideoInfo("https://example.com/watch", html, "id", null);
        assertNotNull(info);
        assertFalse(info.getFormats().isEmpty());
        assertNotNull(info.getTitle());
    }
}
