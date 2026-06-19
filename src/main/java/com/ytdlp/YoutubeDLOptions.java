package com.ytdlp;

public class YoutubeDLOptions {
    private String format = "best";
    private String outputTemplate = "%(title)s [%(id)s].%(ext)s";
    private boolean simulate = false;
    private boolean skipDownload = false;
    private boolean printJson = false;
    private boolean quiet = false;
    private boolean noWarnings = false;
    private String proxy;
    private String cookieFile;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private int socketTimeout = 30;
    private int retries = 3;
    private int bufferSize = 1024;
    private boolean continueDownload = true;
    private String downloadPath = ".";

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getOutputTemplate() {
        return outputTemplate;
    }

    public void setOutputTemplate(String outputTemplate) {
        this.outputTemplate = outputTemplate;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public void setSimulate(boolean simulate) {
        this.simulate = simulate;
    }

    public boolean isSkipDownload() {
        return skipDownload;
    }

    public void setSkipDownload(boolean skipDownload) {
        this.skipDownload = skipDownload;
    }

    public boolean isPrintJson() {
        return printJson;
    }

    public void setPrintJson(boolean printJson) {
        this.printJson = printJson;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public void setQuiet(boolean quiet) {
        this.quiet = quiet;
    }

    public boolean isNoWarnings() {
        return noWarnings;
    }

    public void setNoWarnings(boolean noWarnings) {
        this.noWarnings = noWarnings;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getCookieFile() {
        return cookieFile;
    }

    public void setCookieFile(String cookieFile) {
        this.cookieFile = cookieFile;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isContinueDownload() {
        return continueDownload;
    }

    public void setContinueDownload(boolean continueDownload) {
        this.continueDownload = continueDownload;
    }

    public String getDownloadPath() {
        return downloadPath;
    }

    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;
    }
}
