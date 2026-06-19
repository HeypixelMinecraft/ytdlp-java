# yt-dlp-java

[yt-dlp](https://github.com/yt-dlp/yt-dlp) 的 **纯 Java** 移植版，以 **Java Library** 形式发布，HTTP 层基于 **OkHttp3**。

[![](https://jitpack.io/v/HeypixelMinecraft/ytdlp-java.svg)](https://jitpack.io/#HeypixelMinecraft/ytdlp-java)
- **Gradle 坐标**: `com.yt-dlp:yt-dlp`
- **Java 包名**: `com.ytdlp`

## 多站点支持（纯 Java）

本库通过多层 Java 提取器覆盖大量站点，无需依赖 Python 版 yt-dlp：

| 层级 | 提取器 | 覆盖范围 |
|------|--------|----------|
| 1 | `YoutubeExtractor` / `YoutubePlaylistExtractor` | YouTube 单视频与播放列表 |
| 2 | 专用提取器 | Vimeo、Dailymotion、Bilibili、Reddit、Twitch、SoundCloud |
| 3 | `OEmbedExtractor` | oEmbed 协议支持的站点（Flickr、TED、Streamable 等） |
| 4 | `ManifestSiteExtractor` | **100+ 站点** URL 模式 + 媒体正则（见 `sites.json`） |
| 5 | `DirectUrlExtractor` | 直链 mp4 / m3u8 / mpd |
| 6 | `WebpageExtractor` | 任意网页（OpenGraph、HTML5、JSON-LD、内联 JSON） |
| 7 | `ExternalYtDlpExtractor`（可选） | 调用系统 yt-dlp 二进制作为兜底 |

可通过 `InfoExtractorProvider` SPI 注册自定义 Java 提取器。

## 模块

| 模块 | 说明 |
|------|------|
| `:` (root) | 核心库 `java-library` |
| `:cli` | 可选命令行工具 |

## 依赖引入

```kotlin
dependencies {
    implementation("com.yt-dlp:yt-dlp:0.2.0-SNAPSHOT")
}
```

## API 示例

```java
import com.ytdlp.YoutubeDL;
import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.model.ExtractorResult;

YoutubeDLOptions options = new YoutubeDLOptions();
options.setSkipDownload(true);

try (YoutubeDL ydl = new YoutubeDL(options)) {
    // YouTube
    ExtractorResult yt = ydl.extract("https://www.youtube.com/watch?v=VIDEO_ID", false);

    // Vimeo（专用 Java 提取器）
    ExtractorResult vimeo = ydl.extract("https://vimeo.com/123456789", false);

    // 任意网页（通用 Java 解析）
    ExtractorResult page = ydl.extract("https://example.com/video-page", false);
}
```

启用可选的 yt-dlp 二进制兜底：

```java
options.setExternalYtDlpEnabled(true);
options.setExternalYtDlpPath("yt-dlp");  // 或绝对路径
```

## 构建

```bash
.\gradlew.bat build -x javadoc
```

## CLI

```bash
.\gradlew.bat :cli:run --args="--print-json --skip-download https://vimeo.com/148751763"
```

## 架构

```
com.ytdlp/
├── YoutubeDL
├── extractor/
│   ├── youtube/          # YouTube 原生
│   ├── vimeo/            # 专用站点
│   ├── manifest/         # sites.json 驱动的 100+ 站点
│   ├── oembed/           # oEmbed 协议
│   ├── generic/          # 直链 + 通用网页
│   ├── external/         # 可选 yt-dlp 子进程
│   └── plugin/           # SPI 插件接口
├── downloader/           # HTTP / HLS / DASH
└── networking/           # OkHttp3
```

## 与原版差异

- **已实现（Java）**：多站点提取框架、YouTube、主流平台、100+ manifest 站点、通用网页解析、HLS/DASH 下载器骨架
- **部分实现**：JS 签名解密、PO Token、FFmpeg 合并（代码存在，尚未完全接入主流程）
- **可选**：`ExternalYtDlpExtractor` 可启用原版 yt-dlp 全部 1000+ 站点

完整 1:1 复刻原版全部提取器需持续移植；当前架构支持通过 `sites.json` 和 SPI 不断扩展。

## 许可

参考上游 [yt-dlp](https://github.com/yt-dlp/yt-dlp)（Unlicense）。
