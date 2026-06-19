package com.ytdlp.extractor.oembed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.extractor.common.WebpageMediaFinder;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.Format;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.util.JsonUtils;
import com.ytdlp.util.UrlUtils;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * oEmbed-based extraction covering dozens of providers (Vimeo embeds, Flickr, etc.)
 * per https://oembed.com/providers.json — implemented in Java.
 */
public class OEmbedExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://.+";
    private static final String RESOURCE = "/extractors/oembed-providers.json";
    private static List<OEmbedProvider> providers;

    public OEmbedExtractor() {
        super("oembed", "OEmbed", VALID_URL);
    }

    @Override
    public boolean suitable(String url) {
        return super.suitable(url) && findProvider(url) != null;
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        OEmbedProvider provider = findProvider(url);
        if (provider == null) {
            throw new ExtractorException("No oEmbed provider for URL: " + url, true);
        }

        String endpoint = provider.endpointFor(url);
        String json = ydl.getRequestDirector().downloadString(new Request(endpoint));
        OEmbedResponse response;
        try {
            response = JsonUtils.mapper().readValue(json, OEmbedResponse.class);
        } catch (Exception e) {
            throw new ExtractorException("Invalid oEmbed response from " + provider.getProviderName(), e);
        }

        VideoInfo info = new VideoInfo();
        info.setId(UrlUtils.filenameFromUrl(url));
        info.setTitle(response.title != null ? response.title : info.getId());
        info.setThumbnail(response.thumbnailUrl);
        info.setWebpageUrl(url);
        info.setUrl(url);

        List<Format> formats = new ArrayList<>();
        if (response.url != null && !response.url.isBlank()) {
            Format direct = new Format();
            direct.setUrl(response.url);
            direct.setFormatId("0");
            direct.setExt(UrlUtils.determineExt(response.url, "mp4"));
            direct.setProtocol("https");
            formats.add(direct);
        } else if (response.html != null) {
            formats.addAll(WebpageMediaFinder.findFormats(response.html, url, null));
            if (formats.isEmpty()) {
                var iframe = Jsoup.parse(response.html).selectFirst("iframe[src]");
                if (iframe != null) {
                    String embedUrl = iframe.attr("abs:src");
                    if (!embedUrl.isBlank()) {
                        String embedHtml = downloadWebpage(embedUrl, null);
                        formats.addAll(WebpageMediaFinder.findFormats(embedHtml, embedUrl, null));
                    }
                }
            }
        }

        if (formats.isEmpty()) {
            throw new ExtractorException("oEmbed provider returned no media for: " + url, true);
        }
        info.setFormats(formats);
        info.setExt(formats.get(0).getExt());
        return ExtractorResult.video(info);
    }

    private static OEmbedProvider findProvider(String url) {
        for (OEmbedProvider provider : loadProviders()) {
            if (provider.matches(url)) {
                return provider;
            }
        }
        return null;
    }

    private static List<OEmbedProvider> loadProviders() {
        if (providers != null) {
            return providers;
        }
        try (InputStream in = OEmbedExtractor.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                providers = List.of();
                return providers;
            }
            providers = JsonUtils.mapper().readValue(in, new TypeReference<List<OEmbedProvider>>() {});
            return providers;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load oEmbed providers", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OEmbedResponse {
        public String title;
        public String url;
        public String html;
        public String thumbnailUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OEmbedProvider {
        private String providerName;
        private List<String> urlPatterns = new ArrayList<>();
        private String endpoint;
        private List<Pattern> compiled = new ArrayList<>();

        public String getProviderName() {
            return providerName;
        }

        public void setProviderName(String providerName) {
            this.providerName = providerName;
        }

        public List<String> getUrlPatterns() {
            return urlPatterns;
        }

        public void setUrlPatterns(List<String> urlPatterns) {
            this.urlPatterns = urlPatterns;
            this.compiled = new ArrayList<>();
            for (String pattern : urlPatterns) {
                compiled.add(Pattern.compile(wildcardToRegex(pattern), Pattern.CASE_INSENSITIVE));
            }
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public boolean matches(String url) {
            ensureCompiled();
            for (Pattern pattern : compiled) {
                if (pattern.matcher(url).find()) {
                    return true;
                }
            }
            return false;
        }

        public String endpointFor(String url) {
            String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8);
            if (endpoint.contains("{url}")) {
                return endpoint.replace("{url}", encoded);
            }
            return endpoint + (endpoint.contains("?") ? "&" : "?") + "url=" + encoded;
        }

        private void ensureCompiled() {
            if (compiled.isEmpty() && urlPatterns != null) {
                setUrlPatterns(urlPatterns);
            }
        }

        private static String wildcardToRegex(String pattern) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pattern.length(); i++) {
                char c = pattern.charAt(i);
                if (c == '*') {
                    sb.append(".*");
                } else if ("\\.[]{}()+-^$|".indexOf(c) >= 0) {
                    sb.append('\\').append(c);
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }
    }
}
