package top.weixiansen574.bilibiliArchive.backupitems;

import org.jetbrains.annotations.NotNull;
import top.weixiansen574.bilibiliArchive.BiliUser;
import top.weixiansen574.bilibiliArchive.archive.FavoriteVideoInfo;
import top.weixiansen574.bilibiliArchive.biliApis.*;
import top.weixiansen574.bilibiliArchive.config.VideoBackupConfig;
import top.weixiansen574.bilibiliArchive.config.user.FavoritesBackupConfig;
import top.weixiansen574.bilibiliArchive.task.ContentUpdatePublisher;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import java.io.IOException;
import java.util.*;

public class FavoritesBackup extends VideoBackupItem {
    public static final int ATTR_NORMAL = 0;
    public static final int ATTR_FAILED = 1;
    public static final int ATTR_UP_DELETED = 9;

    private final FavoritesBackupConfig favoritesBackupConfig;
    private final VideoBackupConfig backupConfig;
    public final String favName;
    public final long favId;

    public FavoritesBackup(BiliUser biliUser, FavoritesBackupConfig favoritesBackupConfig, ContentUpdatePublisher updatePublisher) {
        super(biliUser,updatePublisher);
        this.favoritesBackupConfig = favoritesBackupConfig;
        favName = favoritesBackupConfig.favName;
        favId = favoritesBackupConfig.favId;
        backupConfig = archiveManger.getVideoBackupConfig(getConfigId());
    }

    @Override
    public void execBackup() throws Throwable {
        System.out.println("开始备份收藏夹：" + favName + " 的视频");
        List<FavoriteVideoInfo> localFavoriteVideoInfos = archiveManger.getFavoriteVideoInfos(favId);
        List<DetailedFavoriteVideoInfo> latestFavVideos = null;
        if (localFavoriteVideoInfos.size() > 0) {
            latestFavVideos = getLatestVideosByAnchorPoint(localFavoriteVideoInfos.get(0).fav_time);
        } else {
            latestFavVideos = getLatestVideosByAnchorPoint(0);
        }
        //翻转列表，使下载顺序从晚到早
        Collections.reverse(latestFavVideos);
        //剔除已存在的视频，因为你最后一个收藏的视频，下载后，后面移除了再加入就会导致收藏时间变化导致出错
        for (FavoriteVideoInfo localFavoriteVideoInfo : localFavoriteVideoInfos) {
            latestFavVideos.removeIf(latestFavVideo -> localFavoriteVideoInfo.bvid.equals(latestFavVideo.bvid));
        }
        for (DetailedFavoriteVideoInfo latestFavVideo : latestFavVideos) {
            /* latestFavVideo都是你没有下载过的视频
               因为按收藏时间排序，定位点之后的视频，不可能没下载（除非手动删除） */
            downloadAndInsertVideo(latestFavVideo);
        }

        //使用获取收藏夹所有视频的api,获取所有视频并比对本地收藏夹视频列表，看缺哪个，然后下载缺少的（半路下载失败或手动删除数据库记录会出现缺少）
        System.out.println("检查是否有视频遗漏未下载……");
        localFavoriteVideoInfos = archiveManger.getFavoriteVideoInfos(favId);//重新获取本地收藏夹视频列表
        List<FavoriteVideo> allFavoriteVideoList = OkHttpUtil.getData(biliApiService.getFavoriteVideos(favId));
        HashSet<String> bvidSet = new HashSet<>();
        for (FavoriteVideoInfo favoriteVideoInfo : localFavoriteVideoInfos) {
            bvidSet.add(favoriteVideoInfo.bvid);
        }
        for (int i = 0; i < allFavoriteVideoList.size(); i++) {
            FavoriteVideo favoriteVideo = allFavoriteVideoList.get(i);
            if (!bvidSet.contains(favoriteVideo.bvid)) {
                System.out.println("BV号为%s的视频遗漏，下载此项");
                DetailedFavoriteVideoInfo detailedFavoriteVideoInfo = getDetailedFavoriteVideoInfo(favId, favoriteVideo.bvid, i);
                downloadAndInsertVideo(detailedFavoriteVideoInfo);
            }
        }
        System.out.printf("检查完毕，%d个视频已经下载%n",allFavoriteVideoList.size());
        //删除自己移出收藏夹中的内容
        localFavoriteVideoInfos = archiveManger.getFavoriteVideoInfos(favId);
        bvidSet = new HashSet<>();
        for (FavoriteVideo favoriteVideo : allFavoriteVideoList) {
            bvidSet.add(favoriteVideo.bvid);
        }
        for (FavoriteVideoInfo localFavoriteVideoInfo : localFavoriteVideoInfos) {
            String bvid = localFavoriteVideoInfo.bvid;
            if (!bvidSet.contains(bvid)) {
                //TODO 移除视频如果没有引用
                archiveManger.deleteFavoriteVideo(favId, bvid);
                archiveManger.deleteVideoArchiveIfNotNotReferences(localFavoriteVideoInfo);
                System.out.printf("视频%s已被你移出收藏夹[%s]，本地同步删除！%n", bvid, favName);
            }
        }
    }

    private void downloadAndInsertVideo(DetailedFavoriteVideoInfo favVideoInfo) throws IOException, BiliBiliApiException {
        switch (favVideoInfo.attr) {
            case ATTR_NORMAL -> {
                String bvid = favVideoInfo.bvid;
                VideoInfo videoInfo = OkHttpUtil.getData(biliApiService.getVideoInfoByBvid(bvid));
                backupVideo(videoInfo, backupConfig);
                archiveManger.insertFavoriteVideo(favId,bvid,favVideoInfo.id,favVideoInfo.fav_time);
            }
            case ATTR_FAILED -> {
                insertFailedFavVideo(favVideoInfo);
                System.out.printf("收藏夹中BV号为：%s的视频已失效！备份失败！请尽早在视频失效前启动备份姬≡(▔﹏▔)≡%n", favVideoInfo.bvid);
            }
            case ATTR_UP_DELETED -> {
                insertFailedFavVideo(favVideoInfo);
                System.out.printf("收藏夹中BV号为：%s的视频已被UP主删除！备份失败！请尽早在视频失效前启动备份姬≡(▔﹏▔)≡%n", favVideoInfo.bvid);
            }
        }
    }

    /**
     * 翻收藏夹列表直到找到定位点
     *
     * @param fav_time 定位点视频的收藏时间
     * @return 定位点前面的视频列表（也就是未下载的），按收藏时间从新到旧
     */
    private List<DetailedFavoriteVideoInfo> getLatestVideosByAnchorPoint(long fav_time) throws IOException, BiliBiliApiException {
        List<DetailedFavoriteVideoInfo> latestFavoriteVideoInfoList = new ArrayList<>();
        for (int pn = 1; true; pn++) {
            DetailedFavoriteVideoInfosPage page = OkHttpUtil
                    .getData(biliApiService.getFavoriteVideosInDetailed(favId, pn, 20, 0));
            if (page.medias == null || page.medias.size() == 0) {
                return latestFavoriteVideoInfoList;
            }
            for (DetailedFavoriteVideoInfo info : page.medias) {
                if (info.fav_time <= fav_time) {
                    return latestFavoriteVideoInfoList;
                }
                latestFavoriteVideoInfoList.add(info);
            }
        }
    }

    private DetailedFavoriteVideoInfo getDetailedFavoriteVideoInfo(long fid, String bvid, int index) throws IOException, BiliBiliApiException {
        //根据视频位置信息快速定位页码
        int pn = index / 20 + 1;
        DetailedFavoriteVideoInfosPage page = OkHttpUtil
                .getData(biliApiService.getFavoriteVideosInDetailed(fid, pn, 20, 0));
        for (DetailedFavoriteVideoInfo video : page.medias) {
            if (Objects.equals(video.bvid, bvid)) {
                return video;
            }
        }
        //若那一页没有就翻遍整个收藏夹
        System.out.println("在获取失效视频更多信息的时候要翻遍收藏夹，请检查程序逻辑！");
        for (pn = 1; true; pn++) {
            page = OkHttpUtil.getData(biliApiService.getFavoriteVideosInDetailed(fid, pn, 20, 0));
            if (page.medias == null || page.medias.size() == 0) {
                throw new IllegalStateException("出了点意外，在获取收藏夹失效视频更多信息的时候翻遍了收藏夹没有找到那条视频");
            }
            for (DetailedFavoriteVideoInfo video : page.medias) {
                if (Objects.equals(video.bvid, bvid)) {
                    return video;
                }
            }
        }
    }

    private void insertFailedFavVideo(DetailedFavoriteVideoInfo favoriteVideoInfo) {
        archiveManger.insertFailedVideoInfo(favoriteVideoInfo.bvid,
                favoriteVideoInfo.id,
                favoriteVideoInfo.upper.name,
                favoriteVideoInfo.upper.mid,
                favoriteVideoInfo.cnt_info.collect,
                favoriteVideoInfo.cnt_info.play,
                favoriteVideoInfo.cnt_info.danmaku);
        archiveManger.insertFavoriteVideo(favId, favoriteVideoInfo.bvid,
                favoriteVideoInfo.id, favoriteVideoInfo.fav_time);
    }

    @Override
    public int getConfigId() {
        return favoritesBackupConfig.videoBackupConfigId;
    }

    @Override
    public String getDesc() {
        return String.format("用户：%s(%s) 的收藏夹：%s(%s)", biliUser.getUserName(), biliUser.getUid(), favName, favId);
    }
}
