package com.ytdlp.youtube.pot;

import com.ytdlp.YoutubeDLOptions;

/**
 * Returns manually configured PO tokens from options.
 */
public class ConfiguredPoTokenProvider implements PoTokenProvider {
    private final YoutubeDLOptions options;

    public ConfiguredPoTokenProvider(YoutubeDLOptions options) {
        this.options = options;
    }

    @Override
    public String getPlayerPoToken(String videoId, String visitorData, String playerUrl) {
        return options.getPoToken();
    }

    @Override
    public String getGvsPoToken(String videoId, String visitorData, String streamUrl) {
        return options.getGvsPoToken() != null ? options.getGvsPoToken() : options.getPoToken();
    }
}
