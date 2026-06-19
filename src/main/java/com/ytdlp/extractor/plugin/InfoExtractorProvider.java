package com.ytdlp.extractor.plugin;

import com.ytdlp.extractor.InfoExtractor;

import java.util.List;

/**
 * SPI for registering custom Java extractors at runtime.
 * Add {@code META-INF/services/com.ytdlp.extractor.plugin.InfoExtractorProvider}
 * with your implementation class name.
 */
public interface InfoExtractorProvider {
    List<InfoExtractor> createExtractors();
}
