package com.ytdlp.youtube.jsc;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses YouTube signatureCipher and applies decrypted signature parameters.
 */
public final class SignatureCipherDecoder {

    private SignatureCipherDecoder() {
    }

    public static Map<String, String> parseCipher(String cipher) {
        Map<String, String> result = new LinkedHashMap<>();
        if (cipher == null) {
            return result;
        }
        for (String part : cipher.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                String key = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
                result.put(key, value);
            }
        }
        return result;
    }

    public static String buildUrl(String cipher, String decryptedSig) {
        Map<String, String> parts = parseCipher(cipher);
        String baseUrl = parts.get("url");
        if (baseUrl == null) {
            return null;
        }
        String sigKey = parts.getOrDefault("sp", "signature");
        if (decryptedSig != null) {
            String sep = baseUrl.contains("?") ? "&" : "?";
            return baseUrl + sep + sigKey + "=" + urlEncode(decryptedSig);
        }
        return baseUrl;
    }

    public static String applyNParam(String url, String decryptedN) {
        if (url == null || decryptedN == null) {
            return url;
        }
        return url.replaceAll("([?&])n=[^&]*", "$1n=" + urlEncode(decryptedN));
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
