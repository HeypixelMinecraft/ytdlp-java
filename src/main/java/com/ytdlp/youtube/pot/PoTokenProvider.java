package com.ytdlp.youtube.pot;

/**
 * Provides YouTube PO (Proof of Origin) tokens for InnerTube and stream URLs.
 * Ported from yt-dlp's pot/ subsystem.
 */
public interface PoTokenProvider {
    String getPlayerPoToken(String videoId, String visitorData, String playerUrl);

    String getGvsPoToken(String videoId, String visitorData, String streamUrl);
}
