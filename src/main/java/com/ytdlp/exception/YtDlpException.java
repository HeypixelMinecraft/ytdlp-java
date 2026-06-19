package com.ytdlp.exception;

public class YtDlpException extends RuntimeException {
    public YtDlpException(String message) {
        super(message);
    }

    public YtDlpException(String message, Throwable cause) {
        super(message, cause);
    }
}
