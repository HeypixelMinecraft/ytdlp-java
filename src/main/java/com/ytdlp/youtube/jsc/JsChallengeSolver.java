package com.ytdlp.youtube.jsc;

import java.util.List;
import java.util.Map;

/**
 * Solves YouTube n-parameter and signature (s) JS challenges.
 * Ported from yt-dlp's jsc/ subsystem.
 */
public interface JsChallengeSolver {
    boolean isAvailable();

    /**
     * Decrypt n-challenge values embedded in stream URLs.
     */
    Map<String, String> solveN(List<String> challenges, String playerUrl, String videoId);

    /**
     * Decrypt signature (s) values from signatureCipher.
     */
    Map<String, String> solveSig(List<String> challenges, String playerUrl, String videoId);
}
