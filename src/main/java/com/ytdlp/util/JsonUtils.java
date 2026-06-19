package com.ytdlp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }
    }

    public static String getText(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            if (current == null) {
                return null;
            }
            current = current.get(key);
        }
        return current != null && current.isTextual() ? current.asText() : null;
    }

    public static Long getLong(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            if (current == null) {
                return null;
            }
            current = current.get(key);
        }
        if (current == null || current.isNull()) {
            return null;
        }
        if (current.isNumber()) {
            return current.asLong();
        }
        if (current.isTextual()) {
            try {
                return Long.parseLong(current.asText());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Integer getInt(JsonNode node, String... path) {
        Long value = getLong(node, path);
        return value != null ? value.intValue() : null;
    }

    public static List<JsonNode> traverse(JsonNode root, String... path) {
        List<JsonNode> results = new ArrayList<>();
        traverseRecursive(root, path, 0, results);
        return results;
    }

    private static void traverseRecursive(JsonNode node, String[] path, int depth, List<JsonNode> results) {
        if (node == null) {
            return;
        }
        if (depth >= path.length) {
            if (node.isObject() || node.isValueNode()) {
                results.add(node);
            }
            return;
        }
        String key = path[depth];
        if (node.isArray()) {
            for (JsonNode child : node) {
                traverseRecursive(child, path, depth, results);
            }
        } else if (node.isObject()) {
            if (key.equals("...")) {
                for (JsonNode child : node) {
                    traverseRecursive(child, path, depth + 1, results);
                }
            } else {
                traverseRecursive(node.get(key), path, depth + 1, results);
            }
        }
    }

    public static String searchJson(Pattern pattern, String text, String label) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
