package com.ytdlp.extractor;

import com.ytdlp.extractor.bilibili.BilibiliExtractor;
import com.ytdlp.extractor.dailymotion.DailymotionExtractor;
import com.ytdlp.extractor.external.ExternalYtDlpExtractor;
import com.ytdlp.extractor.generic.DirectUrlExtractor;
import com.ytdlp.extractor.generic.WebpageExtractor;
import com.ytdlp.extractor.manifest.ManifestSiteExtractor;
import com.ytdlp.extractor.oembed.OEmbedExtractor;
import com.ytdlp.extractor.plugin.InfoExtractorProvider;
import com.ytdlp.extractor.reddit.RedditExtractor;
import com.ytdlp.extractor.soundcloud.SoundcloudExtractor;
import com.ytdlp.extractor.twitch.TwitchExtractor;
import com.ytdlp.extractor.vimeo.VimeoExtractor;
import com.ytdlp.extractor.youtube.YoutubeExtractor;
import com.ytdlp.extractor.youtube.YoutubePlaylistExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Ordered extractor registry. Native Java extractors cover 100+ manifest sites,
 * oEmbed providers, dedicated platform extractors, and universal webpage parsing.
 */
public class ExtractorRegistry {
    private final Map<String, InfoExtractor> extractors = new LinkedHashMap<>();

    public ExtractorRegistry() {
        this(false);
    }

    public ExtractorRegistry(boolean enableExternalYtDlp) {
        // YouTube (native Java)
        register(new YoutubePlaylistExtractor());
        register(new YoutubeExtractor());

        // Dedicated platform extractors (high fidelity)
        register(new VimeoExtractor());
        register(new DailymotionExtractor());
        register(new BilibiliExtractor());
        register(new RedditExtractor());
        register(new TwitchExtractor());
        register(new SoundcloudExtractor());

        // oEmbed providers
        register(new OEmbedExtractor());

        // Manifest-driven sites (100+ URL patterns)
        register(new ManifestSiteExtractor());

        // Direct media URLs
        register(new DirectUrlExtractor());

        // Universal webpage fallback (any HTTP page)
        register(new WebpageExtractor());

        // SPI plugins
        ServiceLoader.load(InfoExtractorProvider.class).forEach(provider -> {
            for (InfoExtractor ie : provider.createExtractors()) {
                register(ie);
            }
        });

        // Optional subprocess bridge to official yt-dlp
        if (enableExternalYtDlp) {
            register(new ExternalYtDlpExtractor());
        }
    }

    public void register(InfoExtractor extractor) {
        extractors.put(extractor.getIeKey(), extractor);
    }

    public void registerBefore(String beforeKey, InfoExtractor extractor) {
        Map<String, InfoExtractor> ordered = new LinkedHashMap<>();
        for (Map.Entry<String, InfoExtractor> e : extractors.entrySet()) {
            if (e.getKey().equals(beforeKey)) {
                ordered.put(extractor.getIeKey(), extractor);
            }
            ordered.put(e.getKey(), e.getValue());
        }
        extractors.clear();
        extractors.putAll(ordered);
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

    public int size() {
        return extractors.size();
    }
}
