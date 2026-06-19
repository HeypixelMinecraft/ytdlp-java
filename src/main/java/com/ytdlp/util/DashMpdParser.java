package com.ytdlp.util;

import com.ytdlp.model.Fragment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal MPEG-DASH MPD parser for SegmentTemplate / SegmentURL.
 */
public final class DashMpdParser {

    private DashMpdParser() {
    }

    public static List<Fragment> parseSegments(String mpdXml, String baseUrl) {
        List<Fragment> fragments = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(mpdXml.getBytes(StandardCharsets.UTF_8)));

            NodeList segmentUrls = doc.getElementsByTagName("SegmentURL");
            for (int i = 0; i < segmentUrls.getLength(); i++) {
                Element el = (Element) segmentUrls.item(i);
                String media = el.getAttribute("media");
                if (media.isEmpty()) {
                    media = el.getAttribute("url");
                }
                if (!media.isEmpty()) {
                    Fragment f = new Fragment();
                    f.setUrl(resolve(baseUrl, media));
                    fragments.add(f);
                }
            }

            if (fragments.isEmpty()) {
                NodeList templates = doc.getElementsByTagName("SegmentTemplate");
                for (int i = 0; i < templates.getLength(); i++) {
                    Element tmpl = (Element) templates.item(i);
                    String media = tmpl.getAttribute("media");
                    String init = tmpl.getAttribute("initialization");
                    String startNumber = tmpl.getAttribute("startNumber");
                    String timescale = tmpl.getAttribute("timescale");
                    if (init != null && !init.isEmpty()) {
                        Fragment initFrag = new Fragment();
                        initFrag.setUrl(resolve(baseUrl, init.replace("$Number$", startNumber.isEmpty() ? "1" : startNumber)));
                        fragments.add(initFrag);
                    }
                    NodeList sList = tmpl.getParentNode() != null
                            ? ((Element) tmpl.getParentNode()).getElementsByTagName("S")
                            : null;
                    int sn = startNumber.isEmpty() ? 1 : Integer.parseInt(startNumber);
                    if (sList != null) {
                        for (int s = 0; s < sList.getLength(); s++) {
                            Element seg = (Element) sList.item(s);
                            String d = seg.getAttribute("d");
                            Fragment f = new Fragment();
                            String url = media.replace("$Number$", String.valueOf(sn++));
                            f.setUrl(resolve(baseUrl, url));
                            if (!d.isEmpty() && !timescale.isEmpty()) {
                                try {
                                    f.setDuration(Long.parseLong(d) / Long.parseLong(timescale));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                            fragments.add(f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse DASH MPD", e);
        }
        return fragments;
    }

    private static String resolve(String base, String ref) {
        if (ref.startsWith("http")) {
            return ref;
        }
        if (base.endsWith("/")) {
            return base + ref;
        }
        int slash = base.lastIndexOf('/');
        String prefix = slash >= 0 ? base.substring(0, slash + 1) : base + "/";
        return prefix + ref;
    }
}
