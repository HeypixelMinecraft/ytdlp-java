package com.ytdlp.cli;

import com.ytdlp.YoutubeDL;
import com.ytdlp.YoutubeDLOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Command-line entry point ported from yt-dlp's main().
 */
public class CliMain {
    private static final String VERSION = "0.2.0";

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }

        YoutubeDLOptions options = new YoutubeDLOptions();
        List<String> urls = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h", "--help" -> {
                    printHelp();
                    System.exit(0);
                }
                case "--version" -> {
                    System.out.println("yt-dlp-java " + VERSION);
                    System.exit(0);
                }
                case "-f", "--format" -> options.setFormat(nextArg(args, ++i, arg));
                case "-o", "--output" -> options.setOutputTemplate(nextArg(args, ++i, arg));
                case "--simulate", "-s" -> options.setSimulate(true);
                case "--skip-download" -> options.setSkipDownload(true);
                case "--print-json", "-j" -> options.setPrintJson(true);
                case "-q", "--quiet" -> options.setQuiet(true);
                case "--no-warnings" -> options.setNoWarnings(true);
                case "--proxy" -> options.setProxy(nextArg(args, ++i, arg));
                case "--cookies" -> options.setCookieFile(nextArg(args, ++i, arg));
                case "--user-agent" -> options.setUserAgent(nextArg(args, ++i, arg));
                case "-P", "--paths" -> options.setDownloadPath(nextArg(args, ++i, arg));
                case "--no-continue" -> options.setContinueDownload(false);
                case "--no-playlist" -> options.setNoPlaylist(true);
                case "--external-yt-dlp" -> options.setExternalYtDlpEnabled(true);
                case "--no-external-yt-dlp" -> options.setExternalYtDlpEnabled(false);
                case "--yt-dlp-location" -> options.setExternalYtDlpPath(nextArg(args, ++i, arg));
                default -> {
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                        printHelp();
                        System.exit(2);
                    }
                    urls.add(arg);
                }
            }
        }

        if (urls.isEmpty()) {
            System.err.println("Error: No URLs provided");
            printHelp();
            System.exit(1);
        }

        try (YoutubeDL ydl = new YoutubeDL(options)) {
            ydl.download(urls);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String nextArg(String[] args, int index, String flag) {
        if (index >= args.length) {
            System.err.println("Error: " + flag + " requires an argument");
            System.exit(2);
        }
        return args[index];
    }

    private static void printHelp() {
        System.out.println("""
                yt-dlp-java - A Java port of yt-dlp
                
                Usage: yt-dlp-java [OPTIONS] URL [URL...]
                
                Options:
                  -h, --help              Show this help
                  --version               Show version
                  -f, --format FORMAT     Format selection (best, worst, bestaudio, bestvideo, or format ID)
                  -o, --output TEMPLATE   Output filename template (%(title)s, %(id)s, %(ext)s)
                  -j, --print-json        Print video info as JSON
                  -s, --simulate          Simulate download without writing files
                  --skip-download         Do not download video
                  -q, --quiet             Quiet mode
                  --no-warnings           Suppress warnings
                  --proxy URL             Use HTTP proxy
                  --cookies FILE          Netscape cookie file
                  --user-agent UA         Custom User-Agent
                  -P, --paths PATH        Download directory
                  --no-continue           Do not resume partial downloads
                  --no-playlist           Download single video instead of playlist
                  --external-yt-dlp       Enable optional yt-dlp binary fallback (1000+ sites)
                  --no-external-yt-dlp    Disable yt-dlp subprocess fallback (native Java only)
                  --yt-dlp-location PATH  Path to yt-dlp executable (default: search PATH)
                
                Examples:
                  yt-dlp-java "https://www.youtube.com/watch?v=BaW_jenozKc"
                  yt-dlp-java -f best -o "%(title)s.%(ext)s" URL
                  yt-dlp-java --print-json --skip-download URL
                """);
    }
}
