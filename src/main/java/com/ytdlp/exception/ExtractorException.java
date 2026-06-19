package com.ytdlp.exception;

public class ExtractorException extends YtDlpException {
    private final boolean expected;

    public ExtractorException(String message) {
        this(message, false);
    }

    public ExtractorException(String message, boolean expected) {
        super(message);
        this.expected = expected;
    }

    public ExtractorException(String message, Throwable cause) {
        super(message, cause);
        this.expected = false;
    }

    public boolean isExpected() {
        return expected;
    }
}
