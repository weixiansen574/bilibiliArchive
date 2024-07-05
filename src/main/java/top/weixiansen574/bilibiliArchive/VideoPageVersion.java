package top.weixiansen574.bilibiliArchive;

import top.weixiansen574.bilibiliArchive.archive.DownloadedVideoPage;

import java.util.List;

public class VideoPageVersion {
    public VideoPageVersion(long versionTime, List<DownloadedVideoPage> pages) {
        this.versionTime = versionTime;
        this.pages = pages;
    }

    public VideoPageVersion() {
    }

    public long versionTime;
    public List<DownloadedVideoPage> pages;
}
