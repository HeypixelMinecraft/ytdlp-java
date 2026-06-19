package com.ytdlp.extractor.youtube;

import java.util.Map;

public record InnertubeClient(
        String name,
        String clientName,
        String clientVersion,
        int clientId,
        String userAgent,
        Map<String, Object> extraClientFields
) {
    public static InnertubeClient androidVr() {
        return new InnertubeClient(
                "android_vr",
                "ANDROID_VR",
                "1.65.10",
                28,
                "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
                Map.of(
                        "deviceMake", "Oculus",
                        "deviceModel", "Quest 3",
                        "androidSdkVersion", 32,
                        "osName", "Android",
                        "osVersion", "12L"
                )
        );
    }

    public static InnertubeClient ios() {
        return new InnertubeClient(
                "ios",
                "IOS",
                "21.02.3",
                5,
                "com.google.ios.youtube/21.02.3 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
                Map.of(
                        "deviceMake", "Apple",
                        "deviceModel", "iPhone16,2",
                        "osName", "iPhone",
                        "osVersion", "18.3.2.22D82"
                )
        );
    }

    public static InnertubeClient android() {
        return new InnertubeClient(
                "android",
                "ANDROID",
                "21.02.35",
                3,
                "com.google.android.youtube/21.02.35 (Linux; U; Android 11) gzip",
                Map.of(
                        "androidSdkVersion", 30,
                        "osName", "Android",
                        "osVersion", "11"
                )
        );
    }

    public static InnertubeClient tv() {
        return new InnertubeClient(
                "tv",
                "TVHTML5",
                "7.20260114.12.00",
                7,
                "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold (unlike Gecko), Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)",
                Map.of()
        );
    }

    public static InnertubeClient webSafari() {
        return new InnertubeClient(
                "web_safari",
                "WEB",
                "2.20260114.08.00",
                1,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)",
                Map.of()
        );
    }
}
