# yt-dlp-java

[yt-dlp](https://github.com/yt-dlp/yt-dlp) 的 Java 移植版（MVP），以 **Java Library** 形式发布，HTTP 层基于 **OkHttp3**。

[![](https://jitpack.io/v/HeypixelMinecraft/ytdlp-java.svg)](https://jitpack.io/#HeypixelMinecraft/ytdlp-java)
- **Gradle 坐标**: `com.yt-dlp:yt-dlp`
- **Java 包名**: `com.ytdlp`（Java 标识符不允许 `-`，故用 `ytdlp`）

## 模块

| 模块 | 说明 |
|------|------|
| `:` (root) | 核心库 `java-library`，可发布到 Maven |
| `:cli` | 可选命令行工具，依赖核心库 |

## 依赖引入

Gradle:

```kotlin
dependencies {
    implementation("com.yt-dlp:yt-dlp:0.1.0-SNAPSHOT")
}
```

本地构建后引用:

```kotlin
dependencies {
    implementation(project(":"))
}
```

## 库 API 示例

```java
import com.ytdlp.YoutubeDL;
import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.OkHttpClientFactory;
import okhttp3.OkHttpClient;

YoutubeDLOptions options = new YoutubeDLOptions();
options.setSkipDownload(true);

// 使用默认 OkHttpClient（由 options 自动配置）
try (YoutubeDL ydl = new YoutubeDL(options)) {
    VideoInfo info = ydl.extractInfo("https://www.youtube.com/watch?v=VIDEO_ID");
    System.out.println(info.getTitle());
}

// 或注入自定义 OkHttpClient（适合与应用共享连接池）
OkHttpClient client = OkHttpClientFactory.create(options);
try (YoutubeDL ydl = new YoutubeDL(options, client)) {
    VideoInfo info = ydl.extractInfo("https://example.com/video.mp4");
}
```

## 构建

```bash
# 构建库
.\gradlew.bat build

# 发布到本地 Maven（可选）
.\gradlew.bat publishToMavenLocal
```

## CLI（可选）

```bash
.\gradlew.bat :cli:run --args="--print-json --skip-download https://media.w3.org/2010/05/sintel/trailer.mp4"
```

## 架构

```
com.ytdlp/
├── YoutubeDL              # 核心编排器
├── YoutubeDLOptions       # 配置
├── extractor/             # 站点提取器
├── downloader/            # HTTP 文件下载
├── networking/            # OkHttp3 封装（RequestDirector, OkHttpClientFactory）
├── format/                # 格式选择
└── model/                 # VideoInfo, Format
```

## 技术栈

- **OkHttp3** — 所有 HTTP 请求（提取、下载、Cookie、代理）
- **Jackson** — JSON 解析
- **SLF4J** — 日志 API（`compileOnly`，由调用方提供实现）

## 与原版差异

完整 yt-dlp 支持上千个站点、FFmpeg 合并、HLS/DASH、JS 签名解密、PO Token 等。本 MVP 仅实现核心管道和 YouTube 基础提取，后续可逐步扩展。

## 许可

参考上游 [yt-dlp](https://github.com/yt-dlp/yt-dlp)（Unlicense）。
