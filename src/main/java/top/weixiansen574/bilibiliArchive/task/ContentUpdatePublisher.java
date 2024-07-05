package top.weixiansen574.bilibiliArchive.task;

import top.weixiansen574.bilibiliArchive.ArchiveManger;
import top.weixiansen574.bilibiliArchive.BiliUser;
import top.weixiansen574.bilibiliArchive.UserManger;
import top.weixiansen574.bilibiliArchive.VideoPageVersion;
import top.weixiansen574.bilibiliArchive.archive.ArchiveVideoInfo;
import top.weixiansen574.bilibiliArchive.archive.DownloadedVideoPage;
import top.weixiansen574.bilibiliArchive.biliApis.BiliApiService;
import top.weixiansen574.bilibiliArchive.biliApis.BiliBiliApiException;
import top.weixiansen574.bilibiliArchive.biliApis.GeneralResponse;
import top.weixiansen574.bilibiliArchive.biliApis.VideoInfo;
import top.weixiansen574.bilibiliArchive.config.VideoDownloadConfig;
import top.weixiansen574.bilibiliArchive.config.user.CommentUpdateConfig;
import top.weixiansen574.bilibiliArchive.config.user.VideoContentUpdateConfig;
import top.weixiansen574.bilibiliArchive.dbmapper.ContentUpdatePlanMapper;
import top.weixiansen574.bilibiliArchive.downloaders.CommentDownloader;
import top.weixiansen574.bilibiliArchive.downloaders.VideoDownloader;
import top.weixiansen574.bilibiliArchive.task.contentupdate.VideoUpdatePlan;
import top.weixiansen574.bilibiliArchive.util.MiscUtils;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ContentUpdatePublisher {
    TaskManger taskManger;
    UserManger userManger;
    ContentUpdatePlanMapper mapper;
    private PublishThread publishThread;

    public ContentUpdatePublisher(TaskManger taskManger, UserManger userManger, ContentUpdatePlanMapper mapper) {
        this.taskManger = taskManger;
        this.userManger = userManger;
        this.mapper = mapper;
    }

    public void start() {
        if (publishThread == null) {
            publishThread = new PublishThread(userManger, mapper, taskManger,this);
            publishThread.setName("CommunityUpdatePublisher");
            publishThread.start();
        }
    }

    public void stop() {
        if (publishThread != null) {
            publishThread.running = false;
            publishThread.interrupt();
            try {
                publishThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void addVideoUpdatePlan(long uid, VideoInfo videoInfo, VideoDownloadConfig videoConfig, VideoContentUpdateConfig updateConfig) {
        if (updateConfig == null){
            return;
        }
        String bvid = videoInfo.bvid;
        //清理掉bvid相重的更新计划（优先级覆盖时避免之前的任务窜扰）
        mapper.deleteVideoUpdatePlanByBvid(bvid);
        VideoUpdatePlan plan = new VideoUpdatePlan();
        plan.uid = uid;
        plan.bvid = bvid;
        plan.avid = videoInfo.aid;
        plan.title = videoInfo.title;
        plan.video_codec_id = videoConfig.codecId;
        plan.video_quality = videoConfig.clarity;
        plan.update_comment = updateConfig.updateComment ? 1 : 0;
        plan.update_video_and_danmaku = updateConfig.updateVideoAndDanmaku ? 1 : 0;
        CommentUpdateConfig commentUpdate = updateConfig.commentUpdate;

        switch (commentUpdate.mode) {
            case CommentUpdateConfig.MODE_ALL -> {
                plan.comment_root_limit = -1;
                plan.comment_sort_type = CommentDownloader.SORT_BY_TIME;
                plan.comment_time_limit = 0L;
            }
            case CommentUpdateConfig.MODE_TOP_LIKE -> {
                plan.comment_root_limit = commentUpdate.rootLimit;
                plan.comment_sort_type = CommentDownloader.SORT_BY_LIKE;
                plan.comment_time_limit = 0L;
            }
            case CommentUpdateConfig.MODE_TIME_ANCHOR -> {
                plan.comment_root_limit = -1;
                plan.comment_sort_type = CommentDownloader.SORT_BY_TIME;
                plan.comment_time_limit = commentUpdate.timeAnchor;
            }
            default -> throw new IllegalArgumentException("Unknown comment update mode: " + commentUpdate.mode);
        }
        plan.comment_reply_use_preview = commentUpdate.replyUsePreview ? 1 : 0;
        plan.do_not_update_comment_when_a_certain_quantity = commentUpdate.doNotUpdateWhenACertainQuantity;
        long time = System.currentTimeMillis();
        for (int i = 1; i <= updateConfig.loopCount; i++) {
            plan.timestamp = time + (updateConfig.interval * i);
            System.out.printf("内容更新器：将在%s更新视频：%s%n", MiscUtils.cnSdf.format(new Date(plan.timestamp)), plan.bvid);
            mapper.insertVideoUpdatePlan(plan);
        }
        //中断sleep，使其重新查询
        publishThread.interrupt();
    }


    public synchronized void removeUpdatePlans(String bvid){
        mapper.deleteVideoUpdatePlanByBvid(bvid);
    }

    private static String formatTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = ((millis % 3600000) % 60000) / 1000;
        return String.format("%02d时%02d分%02d秒", hours, minutes, seconds);
    }

    @SuppressWarnings("BusyWait")
    private static class PublishThread extends Thread {
        public boolean running;
        public UserManger userManger;
        public ContentUpdatePlanMapper mapper;
        public TaskManger taskManger;
        public ContentUpdatePublisher shelf;

        public PublishThread(UserManger userManger, ContentUpdatePlanMapper mapper,
                             TaskManger taskManger, ContentUpdatePublisher shelf) {
            this.userManger = userManger;
            this.mapper = mapper;
            this.taskManger = taskManger;
            this.shelf = shelf;
        }

        @Override
        public void run() {
            running = true;
            while (running) {
                try {
                    VideoUpdatePlan plan = mapper.getEarliestVideoUpdatePlan();

                    if (plan == null) {
                        synchronized (this) {
                            this.wait();
                        }
                    } else {
                        long sleepTime = plan.timestamp - System.currentTimeMillis();
                        if (sleepTime > 0) {
                            System.out.printf("社区更新器：将在%s后更新视频：[%s][%s]%n",
                                    formatTime(sleepTime) , plan.bvid,plan.title);
                            Thread.sleep(sleepTime);
                            BiliUser biliUser = userManger.getUserByUid(plan.uid);
                            if (biliUser == null){
                                System.out.println("[内容更新器]未找到UID："+plan.uid+" 无法执行更新任务");
                            } else {
                                taskManger.commitTask(new VideoUpdateTask(shelf,biliUser,plan));
                            }

                        } else {
                            System.out.println("更新任务："+plan+"已过期");
                        }
                        mapper.deleteVideoUpdatePlanById(plan.id);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private static class VideoUpdateTask implements Runnable {
        ContentUpdatePublisher shelf;
        BiliUser user;
        VideoUpdatePlan plan;

        public VideoUpdateTask(ContentUpdatePublisher shelf, BiliUser user, VideoUpdatePlan plan) {
            this.shelf = shelf;
            this.user = user;
            this.plan = plan;
        }

        @Override
        public void run() {
            VideoDownloader videoDownloader = user.getVideoDownloader();
            ArchiveManger archiveManger = user.getArchiveManger();
            BiliApiService biliApiService = user.getBiliApiService();
            try {
                ArchiveVideoInfo archiveVideoInfo = archiveManger.getArchiveVideoInfo(plan.bvid);
                String bvid = plan.bvid;
                String title = plan.title;
                if (archiveVideoInfo == null){
                    //移除任务
                    shelf.removeUpdatePlans(bvid);
                    System.out.println("视频："+bvid+" 已被移出存档库，已取消更新任务");
                    return;
                }
                //更新视频
                GeneralResponse<VideoInfo> resp = OkHttpUtil.executeCall(biliApiService.getVideoInfoByBvid(bvid));
                if (!resp.isSuccess()){
                    if (ArchiveManger.checkVideoIsFailedFromCode(resp.code)) {
                        archiveManger.updateVideoToFailed(bvid,resp.code);
                        //移除任务
                        shelf.removeUpdatePlans(bvid);
                        System.out.printf("发现视频[%s][%s]失效，未执行更新，已更新存档状态%n",bvid,title);
                    }
                }
                VideoInfo videoInfo = resp.getDataNotNull();
                System.out.printf("正在更新视频项目[%s][%s]……",bvid,title);
                if (plan.update_video_and_danmaku == 1){
                    System.out.println("正在更新视频……");
                    List<VideoPageVersion> pageVersions = archiveVideoInfo.getPageVersionsObject();
                    int size = pageVersions.size();
                    VideoPageVersion videoPageVersion = pageVersions.get(size - 1);
                    List<DownloadedVideoPage> videoPages = videoDownloader.downloadOrUpdateVideo(videoInfo,
                            videoPageVersion.pages,
                            plan.video_quality, plan.video_codec_id,
                            320 * 1024, user.currentTimeIsVip());
                    ArchiveVideoInfo.addToLastIfInconsistency(pageVersions,videoPages);
                    //更新剧集
                    archiveVideoInfo.setPageVersionsObject(pageVersions);
                    if (size != pageVersions.size()){
                        System.out.println("视频更新完成，检测到视频剧集列表发生变化，已更新！剧集列表版本信息 "+
                                MiscUtils.cnSdf.format(new Date(pageVersions.get(size).versionTime)) +" => "+
                                MiscUtils.cnSdf.format(new Date(pageVersions.get(size+1).versionTime)));
                    } else {
                        System.out.println("视频未更新，剧集列表未发生变化");
                    }
                    System.out.println("正在更新弹幕……");
                    videoDownloader.downloadOrUpdateDanmaku(videoInfo);
                    System.out.println("弹幕更新完成");
                }
                //更新评论
                if(plan.update_comment == 1) {
                    System.out.println("正在更新评论……");
                    videoDownloader.downloadOrUpdateComment(plan.avid, CommentDownloader.TYPE_VIDEO,
                            plan.comment_root_limit,
                            plan.comment_sort_type,
                            plan.comment_time_limit,
                            plan.comment_reply_use_preview == 1);
                    System.out.println("评论更新完成");
                }
                archiveVideoInfo.community_update_time = System.currentTimeMillis();
                archiveVideoInfo.view = videoInfo.stat.view;
                archiveVideoInfo.danmaku = videoInfo.stat.danmaku;
                archiveVideoInfo.favorite = videoInfo.stat.favorite;
                archiveVideoInfo.coin = videoInfo.stat.coin;
                archiveVideoInfo.like = videoInfo.stat.like;
                archiveVideoInfo.share = videoInfo.stat.share;
                archiveVideoInfo.reply = videoInfo.stat.reply;
                archiveManger.updateArchiveVideoInfo(archiveVideoInfo);
                System.out.println("更新完成");
            } catch (IOException | BiliBiliApiException e) {
                e.printStackTrace();
            }
        }
    }
}
