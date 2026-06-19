package com.ytdlp.downloader;

import com.ytdlp.YoutubeDL;
import com.ytdlp.YoutubeDLOptions;
import com.ytdlp.exception.DownloadException;
import com.ytdlp.model.Format;
import com.ytdlp.model.Fragment;
import com.ytdlp.model.VideoInfo;
import com.ytdlp.networking.Request;
import com.ytdlp.networking.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base downloader for segmented media (HLS/DASH).
 * Ported from yt-dlp's FragmentFD.
 */
public abstract class FragmentDownloader implements FileDownloader {
    protected final YoutubeDL ydl;
    protected final YoutubeDLOptions options;

    protected FragmentDownloader(YoutubeDL ydl) {
        this.ydl = ydl;
        this.options = ydl.getOptions();
    }

    protected abstract List<Fragment> resolveFragments(Format format);

    @Override
    public void download(VideoInfo info, Format format, Path destination) {
        List<Fragment> fragments = resolveFragments(format);
        if (fragments.isEmpty()) {
            throw new DownloadException("No fragments found for " + format.getProtocol());
        }

        Path tempFile = Path.of(destination.toString() + ".part");
        int concurrency = Math.max(1, options.getConcurrentFragments());
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        AtomicInteger completed = new AtomicInteger(0);

        try {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
            Files.createFile(tempFile);

            try (OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.APPEND)) {
                for (int i = 0; i < fragments.size(); i += concurrency) {
                    int end = Math.min(i + concurrency, fragments.size());
                    List<Future<byte[]>> futures = new java.util.ArrayList<>();
                    for (int j = i; j < end; j++) {
                        final Fragment frag = fragments.get(j);
                        futures.add(pool.submit(() -> downloadFragment(frag, format)));
                    }
                    for (Future<byte[]> f : futures) {
                        byte[] data = f.get();
                        out.write(data);
                        int done = completed.incrementAndGet();
                        ydl.toScreen(String.format("[download] Fragment %d/%d", done, fragments.size()));
                    }
                }
            }

            Files.move(tempFile, destination,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            ydl.toScreen("[download] Saved fragmented stream to " + destination);
        } catch (Exception e) {
            throw new DownloadException("Fragment download failed: " + e.getMessage(), e);
        } finally {
            pool.shutdown();
        }
    }

    private byte[] downloadFragment(Fragment fragment, Format format) throws IOException {
        Request request = new Request(fragment.getUrl(), "GET", null, format.getHttpHeaders());
        try (Response response = ydl.getRequestDirector().send(request)) {
            if (response.getStatusCode() >= 400) {
                throw new DownloadException("HTTP " + response.getStatusCode() + " for fragment");
            }
            return response.getOkResponse().body().bytes();
        }
    }
}
