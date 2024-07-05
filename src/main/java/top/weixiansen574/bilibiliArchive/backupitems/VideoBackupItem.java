package top.weixiansen574.bilibiliArchive.backupitems;

import com.alibaba.fastjson2.JSON;
import org.jetbrains.annotations.Nullable;
import top.weixiansen574.bilibiliArchive.BiliUser;
import top.weixiansen574.bilibiliArchive.VideoPageVersion;
import top.weixiansen574.bilibiliArchive.archive.ArchiveVideoInfo;
import top.weixiansen574.bilibiliArchive.archive.DownloadedVideoPage;
import top.weixiansen574.bilibiliArchive.biliApis.BiliBiliApiException;
import top.weixiansen574.bilibiliArchive.biliApis.VideoInfo;
import top.weixiansen574.bilibiliArchive.config.CommentDownloadConfig;
import top.weixiansen574.bilibiliArchive.config.VideoBackupConfig;
import top.weixiansen574.bilibiliArchive.config.VideoDownloadConfig;
import top.weixiansen574.bilibiliArchive.downloaders.CommentDownloader;
import top.weixiansen574.bilibiliArchive.task.ContentUpdatePublisher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class VideoBackupItem extends BackupItem {

    protected ContentUpdatePublisher updatePublisher;

    public VideoBackupItem(BiliUser biliUser, ContentUpdatePublisher updatePublisher) {
        super(biliUser);
        this.updatePublisher = updatePublisher;
    }

    public void backupVideo(VideoInfo videoInfo, VideoBackupConfig backupConfig) throws BiliBiliApiException, IOException {
        System.out.printf("正在备份视频%s(%s)……%n",videoInfo.bvid,videoInfo.title);
        boolean downloaded = downloadOrOverwriteVideoArchive(videoInfo, backupConfig.video, backupConfig.comment);
        System.out.printf("视频%s(%s)备份完成！%n",videoInfo.bvid,videoInfo.title);
        if(downloaded && updatePublisher != null){
            updatePublisher.addVideoUpdatePlan(biliUser.getUid(), videoInfo,backupConfig.video,backupConfig.update);
        }
    }

    /**
     * 下载或者按优先级覆盖原有存档的视频
     * @return 是否是新下载或覆盖原有的？
     */
    public boolean downloadOrOverwriteVideoArchive
            (VideoInfo videoInfo, VideoDownloadConfig videoCfg, CommentDownloadConfig commentCfg)
            throws BiliBiliApiException, IOException {
        ArchiveVideoInfo archiveVideoInfo = archiveManger.getArchiveVideoInfo(videoInfo.bvid);
        if (archiveVideoInfo != null) {
            if (archiveVideoInfo.downloading != 0) {
                //上次遇到问题，重新下载，不论原有配置优先级是否更高
                List<VideoPageVersion> pageVersions = archiveVideoInfo.getPageVersionsObject();
                //之前下载遇到问题，为了避免下载中断的视频文件被排除，下载视频应该完全下载
                List<DownloadedVideoPage> downloadedPages = downloadVideoAndAdditions
                        (videoInfo, null, videoCfg, commentCfg);
                archiveVideoInfo.config_id = getConfigId();
                archiveVideoInfo.downloading = ArchiveVideoInfo.DOWNLOAD_STATE_OK;
                if (pageVersions == null) {
                    pageVersions = new ArrayList<>();
                }
                ArchiveVideoInfo.addToLastIfInconsistency(pageVersions, downloadedPages);
                archiveVideoInfo.setPageVersionsObject(pageVersions);
                archiveManger.updateArchiveVideoInfo(archiveVideoInfo);
                return true;
            } else if (checkIfOverwriteNeeded(archiveVideoInfo)) {
                //上次的优比本次的先级低，覆盖下载
                List<VideoPageVersion> pageVersions = archiveVideoInfo.getPageVersionsObject();
                List<DownloadedVideoPage> downloadedPages = downloadVideoAndAdditions
                        (videoInfo, pageVersions.get(pageVersions.size() - 1).pages, videoCfg, commentCfg);
                archiveVideoInfo.config_id = getConfigId();
                archiveVideoInfo.downloading = ArchiveVideoInfo.DOWNLOAD_STATE_OK;
                ArchiveVideoInfo.addToLastIfInconsistency(pageVersions, downloadedPages);
                archiveVideoInfo.setPageVersionsObject(pageVersions);
                archiveManger.updateArchiveVideoInfo(archiveVideoInfo);
                return true;
            } else {
                //上次的优先级更高，跳过下载
                return false;
            }
        }
        //全新下载
        archiveVideoInfo = archiveManger.insertNewDownloadingArcVideoInfo(videoInfo, getConfigId());
        List<DownloadedVideoPage> videoPages = downloadVideoAndAdditions
                (videoInfo, null, videoCfg, commentCfg);
        archiveVideoInfo.downloading = ArchiveVideoInfo.DOWNLOAD_STATE_OK;
        List<VideoPageVersion> pageVersions = new ArrayList<>();
        pageVersions.add(new VideoPageVersion(archiveVideoInfo.save_time, videoPages));
        archiveVideoInfo.setPageVersionsObject(pageVersions);
        archiveManger.updateArchiveVideoInfo(archiveVideoInfo);
        return true;
    }

    protected boolean checkIfOverwriteNeeded(ArchiveVideoInfo archiveVideoInfo) {
        return archiveManger.getVideoCfgPriority(archiveVideoInfo.config_id) >
                archiveManger.getVideoCfgPriority(getConfigId());
    }

    private List<DownloadedVideoPage> downloadVideoAndAdditions
            (VideoInfo videoInfo, @Nullable List<DownloadedVideoPage> videoPages,
             VideoDownloadConfig video, CommentDownloadConfig comment) throws IOException, BiliBiliApiException {

        String bvid = videoInfo.bvid;
        //下载up主头像
        videoDownloader.downloadUpAvatarIfNotExists(videoInfo.owner.face);
        //下载封面
        videoDownloader.downloadCover(videoInfo.pic, bvid);
        //下载视频
        List<DownloadedVideoPage> downloadedVideoPages = videoDownloader.downloadOrUpdateVideo(videoInfo, videoPages,
                video.clarity, video.codecId,
                320 * 1024, biliUser.currentTimeIsVip());
        //下载弹幕
        videoDownloader.downloadOrUpdateDanmaku(videoInfo);
        //下载评论
        int rootLimit = -1;
        int sortType = CommentDownloader.SORT_BY_TIME;
        switch (comment.commentCfg) {
            case CommentDownloadConfig.COMMENT_CFG_ALL:
                break;
            case CommentDownloadConfig.COMMENT_CFG_TOP_LIKE:
                rootLimit = comment.commentTopLike;
                break;
            case CommentDownloadConfig.COMMENT_CFG_NOT_ARCHIVE:
                return downloadedVideoPages;
            default:
                throw new IllegalArgumentException("非法的评论配置：" + JSON.toJSONString(comment));
        }
        videoDownloader.downloadOrUpdateComment(videoInfo.aid, 1, rootLimit, sortType,
                0, comment.replyUsePreview);
        return downloadedVideoPages;
    }
}
