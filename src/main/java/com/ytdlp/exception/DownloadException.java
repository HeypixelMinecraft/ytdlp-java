package com.ytdlp.exception;

public class DownloadException extends YtDlpException {
    public DownloadException(String message) {
        super(message);
    }

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
