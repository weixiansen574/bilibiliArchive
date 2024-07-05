package top.weixiansen574.bilibiliArchive.backupitems;

import org.jetbrains.annotations.NotNull;
import top.weixiansen574.bilibiliArchive.ArchiveManger;
import top.weixiansen574.bilibiliArchive.BiliUser;
import top.weixiansen574.bilibiliArchive.biliApis.BiliApiService;
import top.weixiansen574.bilibiliArchive.downloaders.VideoDownloader;

public abstract class BackupItem implements Comparable<BackupItem>{
    protected BiliUser biliUser;
    protected BiliApiService biliApiService;
    public VideoDownloader videoDownloader;
    public ArchiveManger archiveManger;


    public BackupItem(BiliUser biliUser) {
        this.biliUser = biliUser;
        this.biliApiService = biliUser.getBiliApiService();
        this.archiveManger = biliUser.getArchiveManger();
        this.videoDownloader = biliUser.getVideoDownloader();
    }

    public abstract void execBackup() throws Throwable;

    @Override
    public int compareTo(@NotNull BackupItem other){
        return Integer.compare(archiveManger.getVideoCfgPriority(this.getConfigId()),
                archiveManger.getVideoCfgPriority(other.getConfigId()));
    };

    public abstract int getConfigId();

    public abstract String getDesc();
}
