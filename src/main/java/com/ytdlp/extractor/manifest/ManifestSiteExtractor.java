package com.ytdlp.extractor.manifest;

import com.ytdlp.exception.ExtractorException;
import com.ytdlp.extractor.InfoExtractor;
import com.ytdlp.extractor.common.WebpageMediaFinder;
import com.ytdlp.model.ExtractorResult;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.util.UrlUtils;

import java.util.regex.Matcher;

/**
 * Handles hundreds of sites via a bundled manifest of URL patterns and media regexes.
 * Each entry mirrors yt-dlp's simple regex-based extractors, implemented in Java.
 */
public class ManifestSiteExtractor extends InfoExtractor {
    private static final String VALID_URL = "(?i)https?://.+";

    public ManifestSiteExtractor() {
        super("manifest", "Manifest", VALID_URL);
    }

    @Override
    public boolean suitable(String url) {
        return super.suitable(url) && SiteManifest.findForUrl(url) != null;
    }

    @Override
    protected ExtractorResult realExtract(String url) {
        SiteDefinition site = SiteManifest.findForUrl(url);
        if (site == null) {
            throw new ExtractorException("No manifest entry for URL: " + url, true);
        }

        String html = downloadWebpage(url, null);
        String id = extractId(url);
        VideoInfo info = WebpageMediaFinder.buildVideoInfo(url, html, id, site.getMediaRegexes());
        if (info == null || info.getFormats().isEmpty()) {
            throw new ExtractorException("No media found for " + site.getIeName() + ": " + url, true);
        }
        info.setExtractor(site.getIeName());
        info.setExtractorKey(site.getIeKey());
        return ExtractorResult.video(info);
    }

    private static String extractId(String url) {
        Matcher matcher = java.util.regex.Pattern.compile("/([^/?#]+)(?:[?#]|$)").matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return UrlUtils.filenameFromUrl(url);
    }
}
