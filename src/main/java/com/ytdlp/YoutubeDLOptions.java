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
    private boolean noPlaylist = false;

    // JS challenge solver (yt-dlp-ejs)
    private String jsRuntime;
    private String ejsScriptPath;
    private int jsSolverTimeout = 60;

    // PO Token
    private String poToken;
    private String gvsPoToken;
    private String visitorData;

    // HLS/DASH
    private int concurrentFragments = 4;

    // FFmpeg merge
    private String ffmpegLocation;
    private String mergeOutputFormat = "mp4";

    // External yt-dlp subprocess bridge (optional; disabled by default — use pure Java extractors)
    private boolean externalYtDlpEnabled = false;
    private String externalYtDlpPath;
    private long externalYtDlpTimeout = 120;

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

    public boolean isNoPlaylist() {
        return noPlaylist;
    }

    public void setNoPlaylist(boolean noPlaylist) {
        this.noPlaylist = noPlaylist;
    }

    public String getJsRuntime() {
        return jsRuntime;
    }

    public void setJsRuntime(String jsRuntime) {
        this.jsRuntime = jsRuntime;
    }

    public String getEjsScriptPath() {
        return ejsScriptPath;
    }

    public void setEjsScriptPath(String ejsScriptPath) {
        this.ejsScriptPath = ejsScriptPath;
    }

    public int getJsSolverTimeout() {
        return jsSolverTimeout;
    }

    public void setJsSolverTimeout(int jsSolverTimeout) {
        this.jsSolverTimeout = jsSolverTimeout;
    }

    public String getPoToken() {
        return poToken;
    }

    public void setPoToken(String poToken) {
        this.poToken = poToken;
    }

    public String getGvsPoToken() {
        return gvsPoToken;
    }

    public void setGvsPoToken(String gvsPoToken) {
        this.gvsPoToken = gvsPoToken;
    }

    public String getVisitorData() {
        return visitorData;
    }

    public void setVisitorData(String visitorData) {
        this.visitorData = visitorData;
    }

    public int getConcurrentFragments() {
        return concurrentFragments;
    }

    public void setConcurrentFragments(int concurrentFragments) {
        this.concurrentFragments = concurrentFragments;
    }

    public String getFfmpegLocation() {
        return ffmpegLocation;
    }

    public void setFfmpegLocation(String ffmpegLocation) {
        this.ffmpegLocation = ffmpegLocation;
    }

    public String getMergeOutputFormat() {
        return mergeOutputFormat;
    }

    public void setMergeOutputFormat(String mergeOutputFormat) {
        this.mergeOutputFormat = mergeOutputFormat;
    }

    public boolean isExternalYtDlpEnabled() {
        return externalYtDlpEnabled;
    }

    public void setExternalYtDlpEnabled(boolean externalYtDlpEnabled) {
        this.externalYtDlpEnabled = externalYtDlpEnabled;
    }

    public String getExternalYtDlpPath() {
        return externalYtDlpPath;
    }

    public void setExternalYtDlpPath(String externalYtDlpPath) {
        this.externalYtDlpPath = externalYtDlpPath;
    }

    public long getExternalYtDlpTimeout() {
        return externalYtDlpTimeout;
    }

    public void setExternalYtDlpTimeout(long externalYtDlpTimeout) {
        this.externalYtDlpTimeout = externalYtDlpTimeout;
    }
}
