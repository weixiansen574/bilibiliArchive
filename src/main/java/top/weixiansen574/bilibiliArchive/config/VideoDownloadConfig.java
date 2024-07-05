package top.weixiansen574.bilibiliArchive.config;

import top.weixiansen574.bilibiliArchive.biliApis.VideoInfoInterface;

import java.util.Objects;

public class VideoDownloadConfig implements VideoInfoInterface {
    public int clarity;
    public int codecId;

    public VideoDownloadConfig(int clarity, int codecId) {
        this.clarity = clarity;
        this.codecId = codecId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VideoDownloadConfig that = (VideoDownloadConfig) o;
        return clarity == that.clarity && codecId == that.codecId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clarity, codecId);
    }
}
