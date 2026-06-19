package com.ytdlp.util;

import com.ytdlp.model.Fragment;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3u8Parser {

    private static final Pattern URI_LINE = Pattern.compile("URI=\"([^\"]+)\"");

    private M3u8Parser() {
    }

    public static List<Fragment> parseSegments(String manifest, String baseUrl) {
        List<Fragment> fragments = new ArrayList<>();
        String base = baseUrl;
        try {
            URI uri = URI.create(baseUrl);
            String path = uri.getPath();
            int slash = path.lastIndexOf('/');
            if (slash >= 0) {
                base = uri.getScheme() + "://" + uri.getHost() + path.substring(0, slash + 1);
            }
        } catch (Exception ignored) {
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(manifest))) {
            String line;
            Double duration = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#EXTINF:")) {
                    String dur = line.substring(8).split(",")[0].trim();
                    try {
                        duration = Double.parseDouble(dur);
                    } catch (NumberFormatException ignored) {
                        duration = null;
                    }
                } else if (!line.startsWith("#") && !line.isEmpty()) {
                    Fragment frag = new Fragment();
                    frag.setUrl(resolveUrl(base, line));
                    if (duration != null) {
                        frag.setDuration(duration.longValue());
                    }
                    fragments.add(frag);
                    duration = null;
                } else if (line.startsWith("#EXT-X-MAP:")) {
                    Matcher m = URI_LINE.matcher(line);
                    if (m.find()) {
                        Fragment init = new Fragment();
                        init.setUrl(resolveUrl(base, m.group(1)));
                        fragments.add(0, init);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse m3u8", e);
        }
        return fragments;
    }

    private static String resolveUrl(String base, String ref) {
        if (ref.startsWith("http://") || ref.startsWith("https://")) {
            return ref;
        }
        if (ref.startsWith("/")) {
            try {
                URI b = URI.create(base);
                return b.getScheme() + "://" + b.getHost() + ref;
            } catch (Exception e) {
                return ref;
            }
        }
        return base + ref;
    }
}
