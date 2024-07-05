package top.weixiansen574.bilibiliArchive.backupitems;

import okhttp3.ResponseBody;
import top.weixiansen574.bilibiliArchive.ArchiveManger;
import top.weixiansen574.bilibiliArchive.BiliUser;
import top.weixiansen574.bilibiliArchive.archive.ArchiveVideoInfo;
import top.weixiansen574.bilibiliArchive.archive.HistoryVideoInfo;
import top.weixiansen574.bilibiliArchive.biliApis.*;
import top.weixiansen574.bilibiliArchive.config.BlackListUser;
import top.weixiansen574.bilibiliArchive.config.VideoBackupConfig;
import top.weixiansen574.bilibiliArchive.config.user.HistoryBackupConfig;
import top.weixiansen574.bilibiliArchive.task.ContentUpdatePublisher;
import top.weixiansen574.bilibiliArchive.util.MiscUtils;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import java.io.IOException;
import java.util.*;

public class HistoryBackup extends VideoBackupItem {
    private final HistoryBackupConfig backupConfig;
    private final Set<Long> blackListUidSet = new HashSet<>();
    private final VideoBackupConfig videoBackupConfig;

    public HistoryBackup(BiliUser biliUser, HistoryBackupConfig backupConfig, List<BlackListUser> globalUpBlackList,
                         ContentUpdatePublisher updatePublisher) {
        super(biliUser,updatePublisher);
        this.backupConfig = backupConfig;
        videoBackupConfig = archiveManger.getVideoBackupConfig(getConfigId());
        if (globalUpBlackList != null) {
            for (BlackListUser blackListUser : globalUpBlackList) {
                blackListUidSet.add(blackListUser.uid);
                //blackListUsers.put(blackListUser.uid,blackListUser);
            }
        }
        if (backupConfig.upBlackList != null) {
            for (BlackListUser blackListUser : backupConfig.upBlackList) {
                blackListUidSet.add(blackListUser.uid);
                //blackListUsers.put(blackListUser.uid,blackListUser);
            }
        }
    }

    @Override
    public void execBackup() throws Throwable {
        List<HistoryVideoInfo> localHistoryVideoList = archiveManger.getNotFailedHistoryVideos(biliUser.getUid());
        if (localHistoryVideoList.size() == 0) {
            System.out.println("第一次启动历史记录备份，下载第一个视频");
            downloadFirstVideo();
            System.out.println("第一个视频下载完毕！");
            return;
        }
        //获取该时间点往前的视频
        List<HistoriesPage.HistoryItem> newHistoryVideos = getNewHistoryVideos(localHistoryVideoList.get(0).view_at);
        //创建去重用set集合
        HashSet<String> localHistoryVideoBvidSet = new HashSet<>();
        for (HistoryVideoInfo historyVideo : localHistoryVideoList) {
            localHistoryVideoBvidSet.add(historyVideo.bvid);
        }
        //补充失效信息（如果收藏夹里有）
        //获取视频信息（筛掉失效的，备份姬启动前失效的视频就不以“在备份前失效”保存到档案库里了）
        List<PendingDownloadVideo> pendingDownloadVideos = getPendingDownloadVideos(newHistoryVideos);
        List<HistoryVideoInfo> pendingDeleteHistoryVideoList = new ArrayList<>();
        String deleteMethodDesc = null;
        switch (backupConfig.deleteMethod) {
            case HistoryBackupConfig.DELETE_METHOD_ITEM_QUANTITY -> {
                deleteMethodDesc = String.format("按照条目上限%d条", backupConfig.deleteByItemQuantity);
                int currentQuantity = 0;
                //遍历待下载视频，移除超出限制的部分
                List<PendingDownloadVideo> pendingDownloadVideos2 = new ArrayList<>();
                for (PendingDownloadVideo pendingDownloadVideo : pendingDownloadVideos) {
                    if (currentQuantity <= backupConfig.deleteByItemQuantity) {
                        pendingDownloadVideos2.add(pendingDownloadVideo);
                    }
                    currentQuantity++;
                }
                //反转并下载
                Collections.reverse(pendingDownloadVideos2);
                for (PendingDownloadVideo pendingDownloadVideo : pendingDownloadVideos2) {
                    downloadNewOrUpdateViewAt(pendingDownloadVideo, localHistoryVideoBvidSet);
                }
                for (HistoryVideoInfo historyVideo : localHistoryVideoList) {
                    currentQuantity++;
                    if (currentQuantity > backupConfig.deleteByItemQuantity) {
                        pendingDeleteHistoryVideoList.add(historyVideo);
                    }
                }
            }
            case HistoryBackupConfig.DELETE_METHOD_DAYS -> {
                deleteMethodDesc = String.format("按照观看的日期，删除%d天前观看的视频", backupConfig.deleteByDays);
                long expirationTimeStamp = System.currentTimeMillis() / 1000 - backupConfig.getDeleteByDaysTotalTimeSec();
                Collections.reverse(pendingDownloadVideos);//翻转待下载视频列表，使得下载顺序非倒序
                for (PendingDownloadVideo pendingDownloadVideo : pendingDownloadVideos) {
                    if (pendingDownloadVideo.historyItem.view_at > expirationTimeStamp) {
                        downloadNewOrUpdateViewAt(pendingDownloadVideo, localHistoryVideoBvidSet);
                    }
                }
                for (HistoryVideoInfo historyVideo : localHistoryVideoList) {
                    if (historyVideo.view_at < expirationTimeStamp) {
                        pendingDeleteHistoryVideoList.add(historyVideo);
                    }
                }
            }
            case HistoryBackupConfig.DELETE_METHOD_DISK_USAGE -> {
                //需要先下载完新的视频才能统计硬盘占用
                Collections.reverse(pendingDownloadVideos);
                for (PendingDownloadVideo pendingDownloadVideo : pendingDownloadVideos) {
                    downloadNewOrUpdateViewAt(pendingDownloadVideo, localHistoryVideoBvidSet);
                }
                //重新获取本地历史记录视频
                localHistoryVideoList = archiveManger.getNotFailedHistoryVideos(biliUser.getUid());
                long diskUsage = 0;
                for (HistoryVideoInfo historyVideo : localHistoryVideoList) {
                    diskUsage += archiveManger.calculateVideosHardDiskUsage(historyVideo.bvid);
                    if (diskUsage > backupConfig.deleteByDiskUsage) {
                        pendingDeleteHistoryVideoList.add(historyVideo);
                    }
                }
                deleteMethodDesc = String.format("按照硬盘占用，删除超过%.2fGB部分的视频，当前历史记录视频总占用空间：%.2fGB",
                        (float) backupConfig.deleteByDiskUsage / 1024 / 1024 / 1024, (float) diskUsage / 1024 / 1024 / 1024);
            }
        }

        System.out.println("准备删除过期视频，当前过期判定：" + deleteMethodDesc);
        for (HistoryVideoInfo historyVideo : pendingDeleteHistoryVideoList) {
            String bvid = historyVideo.bvid;
            GeneralResponse<VideoInfo> resp = OkHttpUtil.executeCall(biliApiService.getVideoInfoByBvid(bvid));
            //删除掉未失效的视频，保留失效的
            if (resp.isSuccess()) {
                archiveManger.deleteHistoryVideo(biliUser.getUid(), bvid);
                archiveManger.deleteVideoArchiveIfNotNotReferences(historyVideo);
                System.out.printf("已删除过期且未失效的历史记录视频：[%s][%s](于%s观看)%n", bvid, resp.data.title,
                        MiscUtils.cnSdf.format(historyVideo.view_at * 1000));
                //失效的永久保留
            } else if (ArchiveManger.checkVideoIsFailedFromCode(resp.code)) {
                archiveManger.updateVideoToFailed(bvid, resp.code);
                System.out.printf("发现历史记录视频已失效：[%s][%s]%n", bvid, historyVideo.title);
            } else {
                throw new RuntimeException("无法判断视频是否失效：message=" + resp.message + " code=" + resp.code);
            }
        }
        System.out.println("已完成历史记录视频备份！");
    }

    private void downloadFirstVideo() throws IOException, BiliBiliApiException {
        HistoriesPage histories = OkHttpUtil.getData(biliApiService.getLatestVideoHistories(20));
        if (histories != null) {
            HistoriesPage.HistoryItem historyItem = histories.list.get(0);
            String bvid = historyItem.history.bvid;
            VideoInfo videoInfo = OkHttpUtil.getData(biliApiService.getVideoInfoByBvid(bvid));
            backupVideo(videoInfo,videoBackupConfig);
            archiveManger.insertHistoryVideo(biliUser.getUid(),bvid,videoInfo.aid,historyItem.view_at);
        } else {
            System.out.println("下载第一条历史记录失败");
        }
    }

    private List<PendingDownloadVideo> getPendingDownloadVideos(List<HistoriesPage.HistoryItem> newHistoryVideos) throws IOException {
        List<PendingDownloadVideo> pendingDownloadVideos = new ArrayList<>();
        for (HistoriesPage.HistoryItem newHistoryVideo : newHistoryVideos) {
            String bvid = newHistoryVideo.history.bvid;
            GeneralResponse<VideoInfo> resp = OkHttpUtil.executeCall(biliApiService.getVideoInfoByBvid(bvid));
            if (resp.isSuccess()) {
                VideoInfo videoInfo = resp.data;
                if (backupConfig.doNotDownloadVideoReleasedAFewDaysAgo != -1) {
                    long time = System.currentTimeMillis() / 1000 - ((backupConfig.doNotDownloadVideoReleasedAFewDaysAgo) * 24 * 60 * 60);
                    if (videoInfo.ctime > time) {
                        pendingDownloadVideos.add(new PendingDownloadVideo(videoInfo, newHistoryVideo));
                    } else {
                        System.out.printf("视频%s(%s)发布时间过老，已过滤！%n", videoInfo.bvid, videoInfo.title);
                    }
                } else {
                    pendingDownloadVideos.add(new PendingDownloadVideo(videoInfo, newHistoryVideo));
                }

            } else if (resp.code == VideoInfo.VIDEO_FAILED_CODE || resp.code == VideoInfo.VIDEO_FAILED_CODE_UP_DELETED) {
                ArchiveVideoInfo archiveVideoInfo = archiveManger.getArchiveVideoInfo(bvid);
                //若收藏夹的视频在备份前失效无法备份，保存了失效档案，历史记录包含封面与标题，那这里继续补充失效档案的标题和封面
                if (archiveVideoInfo != null && archiveVideoInfo.state == ArchiveVideoInfo.STATE_FAILED_AND_NO_BACKUP) {
                    String coverUrl = newHistoryVideo.cover;
                    archiveManger.supplementaryFailedVideoInfo(bvid, newHistoryVideo.title, newHistoryVideo.tag_name, coverUrl);
                    System.out.println("正在补充未存档失效视频的封面……");
                    videoDownloader.downloadCover(coverUrl,bvid);
                }
            } else {
                throw new RuntimeException("无法判断视频是否失效：message=" + resp.message + " code=" + resp.code);
            }
        }
        return pendingDownloadVideos;
    }

    private List<HistoriesPage.HistoryItem> getNewHistoryVideos(long view_at) throws IOException, BiliBiliApiException {
        List<HistoriesPage.HistoryItem> newHistoryItemList = new ArrayList<>();
        HistoriesPage page = OkHttpUtil.getData(biliApiService.getLatestVideoHistories(20));
        while (true) {
            for (HistoriesPage.HistoryItem historyItem : page.list) {
                if (historyItem.view_at > view_at) {
                    if (historyItem.history.bvid != null && !historyItem.history.bvid.equals("")) {//番剧的bvid为空，存档姬不备份番剧！
                        if (!blackListUidSet.contains(historyItem.author_mid)) {
                            newHistoryItemList.add(historyItem);
                        } else {
                            System.out.printf("由于您设置了此UP主的备份黑名单，UP主%s(%s)的视频：%s(%s)跳过备份！%n",
                                    historyItem.author_mid, historyItem.author_name,
                                    historyItem.history.bvid, historyItem.title);
                        }
                    }
                } else {
                    return newHistoryItemList;
                }
            }
            if (page.list.size() < 1) {
                return newHistoryItemList;
            }
            page = OkHttpUtil.getData(biliApiService.getVideoHistories(20, page.cursor.max, page.cursor.view_at));
        }
    }

    /**
     * 下载视频，若本地历史记录已有本条记录，则更新view_at
     *
     * @param pendingDownloadVideo
     * @throws IOException
     */
    private void downloadNewOrUpdateViewAt(PendingDownloadVideo pendingDownloadVideo, HashSet<String> localHistoryVideoBvidSet) throws IOException, BiliBiliApiException {
        HistoriesPage.HistoryItem historyItem = pendingDownloadVideo.historyItem;
        if (localHistoryVideoBvidSet.contains(historyItem.history.bvid)) {
            //若本地历史记录有本条视频，那么说明视频被再次观看了一遍，更新view_at即可
            System.out.printf("历史记录视频：%s(%s)被你重新观看了一遍，仅更新观看时间(view_at)%n",
                    historyItem.title, historyItem.history.bvid);
            archiveManger.updateHisVideoViewAt(historyItem.view_at, historyItem.history.bvid);
        } else {
            backupVideo(pendingDownloadVideo.videoInfo, videoBackupConfig);
            archiveManger.insertHistoryVideo(biliUser.getUid(), historyItem.history.bvid, historyItem.history.oid, historyItem.view_at);
            System.out.printf("历史记录视频:%s(%s)下载完成！%n", pendingDownloadVideo.videoInfo.bvid, pendingDownloadVideo.videoInfo.title);
        }
    }

    @Override
    public int getConfigId() {
        return backupConfig.videoBackupConfigId;
    }

    @Override
    public String getDesc() {
        return String.format("用户：%s(%s)的历史记录", biliUser.getUserName(), biliUser.getUid());
    }

    private static class PendingDownloadVideo {

        public PendingDownloadVideo(VideoInfo videoInfo, HistoriesPage.HistoryItem historyItem) {
            this.videoInfo = videoInfo;
            this.historyItem = historyItem;
        }

        VideoInfo videoInfo;
        HistoriesPage.HistoryItem historyItem;
    }
}
