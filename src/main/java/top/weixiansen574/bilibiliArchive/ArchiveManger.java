package top.weixiansen574.bilibiliArchive;

import org.apache.ibatis.cursor.Cursor;
import top.weixiansen574.bilibiliArchive.archive.*;
import top.weixiansen574.bilibiliArchive.biliApis.BiliComment;
import top.weixiansen574.bilibiliArchive.biliApis.VideoInfo;
import top.weixiansen574.bilibiliArchive.config.VideoBackupConfig;
import top.weixiansen574.bilibiliArchive.dbmapper.ArchiveMapper;
import top.weixiansen574.bilibiliArchive.dbmapper.CommentMapper;
import top.weixiansen574.bilibiliArchive.dbmapper.versionctrl.ArchiveInfosDBVersionControllerForSQLite;
import top.weixiansen574.bilibiliArchive.dbmapper.versionctrl.CommentDBVersionController;
import top.weixiansen574.bilibiliArchive.exceptions.IllegalConfigException;
import top.weixiansen574.bilibiliArchive.util.FFmpegUtil;
import top.weixiansen574.bilibiliArchive.util.FileUtil;
import top.weixiansen574.bilibiliArchive.util.MiscUtils;
import top.weixiansen574.db.MapperCreator;
import top.weixiansen574.db.MapperCreatorForSQLite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArchiveManger {
    private final File archivePath;
    private final File videosPath;
    private final File upAvatarPath;
    private final File commentPicturePath;
    public final File cachePath;
    public final CommentMapper commentMapper;
    public final ArchiveMapper archiveMapper;
    public List<Integer> videoConfigIds;

    public ArchiveManger(File archivePath, File cachePath, List<Integer> videoConfigIds) throws IOException, SQLException, ConfigListMismatchException {
        this.archivePath = archivePath;
        this.cachePath = cachePath;
        this.videosPath = FileUtil.getOrCreateDir(archivePath, "Videos");
        this.upAvatarPath = FileUtil.getOrCreateDir(archivePath, "UploaderAvatars");
        this.commentPicturePath = FileUtil.getOrCreateDir(archivePath, "CommentPictures");
        MapperCreator mapperCreator = new MapperCreatorForSQLite(new Class[]{CommentMapper.class},
                new File(archivePath, "comments.db"), new CommentDBVersionController());
        this.commentMapper = mapperCreator.getMapper(CommentMapper.class);
        MapperCreator arcMapperCreator = new MapperCreatorForSQLite(new Class[]{ArchiveMapper.class},
                new File(archivePath, "archiveInfos.db"), new ArchiveInfosDBVersionControllerForSQLite());
        archiveMapper = arcMapperCreator.getMapper(ArchiveMapper.class);
        setVideoCfgIds(videoConfigIds);
    }

    public boolean mergeAndSaveVideoFile(String bvid, long cid, File videoFile, File audioFile) throws IOException {
        File cidFolder = newBvidCidDirFile(bvid, cid);

        File mergedVideoFile = new File(cidFolder, "video.mp4");
        //如果已经存在，要删除，否则ffmpeg是否确认覆盖卡死（更新视频画质的情况）
        FileUtil.deleteOneFile(mergedVideoFile);
        return FFmpegUtil.merge(audioFile.getAbsolutePath(), videoFile.getAbsolutePath(),
                mergedVideoFile.getAbsolutePath());
    }

    public boolean saveNoAudioVideoFile(String bvid, long cid, File videoFile) throws IOException {
        File cidFolder = newBvidCidDirFile(bvid, cid);
        return videoFile.renameTo(new File(cidFolder, "video.mp4"));
    }

    public File newDanmakuFile(String bvid, long cid) throws IOException {
        File cidFolder = newBvidCidDirFile(bvid, cid);
        return new File(cidFolder, "danmaku.xml");
    }

    public File newBvidCidDirFile(String bvid, long cid) throws IOException {
        File bvidFolder = FileUtil.getOrCreateDir(getVideosPath(), bvid);
        return FileUtil.getOrCreateDir(bvidFolder, "c_" + cid);
    }

    public File newCoverFile(String bvid) throws IOException {
        File bvidFolder = FileUtil.getOrCreateDir(getVideosPath(), bvid);
        return new File(bvidFolder, "cover.jpg");
    }

    public File newUploaderAvatarFile(String fileName) {
        return new File(upAvatarPath, fileName);
    }

    public File newCommentPictureFile(String fileName) {
        return new File(commentPicturePath, fileName);
    }

    public void saveCommentPicture(String fileName, InputStream inputStream) throws IOException {
        FileUtil.outputToFile(inputStream, new File(commentPicturePath, fileName));
    }

    public boolean checkCommentAvatarExists(String avatarName) {
        return commentMapper.checkAvatarIsExists(avatarName);
    }

    public void insertCommentAvatar(CMAvatar cmAvatar) {
        commentMapper.insertAvatar(cmAvatar);
    }


    public void insertCommentOrUpdateLike(BiliComment comment) {
        if (commentMapper.checkCommentIsExists(comment.rpid)) {
            commentMapper.updateCommentLike(comment.like, comment.rpid);
        } else {
            commentMapper.insertComment(new ArchiveComment(comment));
        }
    }

    /**
     * 删除视频信息，如果没有收藏夹历史记录等引用
     *
     * @return true:删除成功 false:有引用，没删除;没有bvid对应的视频
     */
    public boolean deleteVideoInfoIfNotReferences(String bvid) {
        return archiveMapper.deleteVideoInfoIfNotReferences(bvid) != 0;
    }

    public ArchiveVideoInfo insertNewDownloadingArcVideoInfo(VideoInfo videoInfo, int config_id) {
        long time = System.currentTimeMillis();
        ArchiveVideoInfo archiveVideoInfo = new ArchiveVideoInfo(videoInfo, null,
                ArchiveVideoInfo.STATE_NORMAL, time, config_id, time,
                ArchiveVideoInfo.DOWNLOAD_STATE_DOWNLOADING);
        archiveMapper.insertArchiveVideoInfo(archiveVideoInfo);
        return archiveVideoInfo;
    }

    public void updateVideoInfoDownloadStatus(int status, String bvid) {
        archiveMapper.updateDownloadStatus(status, bvid);
    }

    public void updateArchiveVideoInfo(ArchiveVideoInfo archiveVideoInfo) {
        archiveMapper.updateArchiveVideoInfo(archiveVideoInfo);
    }

    public ArchiveVideoInfo getArchiveVideoInfo(String bvid) {
        return archiveMapper.getArchiveVideoInfo(bvid);
    }


    public File getVideosPath() {
        return videosPath;
    }

    public File getArchivePath() {
        return archivePath;
    }

    public File getCachePath() {
        return cachePath;
    }

    private void checkVideoIds(List<Integer> videoCfgIds) throws ConfigListMismatchException {
        List<VideoBackupConfigInfo> allVideoBackupConfig = archiveMapper.getAllVideoBackupConfig();
        if (allVideoBackupConfig.size() != videoCfgIds.size()) {
            throw new ConfigListMismatchException("视频配置id优先级列表与数据库中的不匹配！");
        }
        HashSet<Integer> vidSet = new HashSet<>(videoCfgIds);
        for (VideoBackupConfigInfo videoBackupConfigInfo : allVideoBackupConfig) {
            if (!vidSet.contains(videoBackupConfigInfo.getId())) {
                throw new ConfigListMismatchException("视频配置id优先级列表与数据库中的不匹配！");
            }
        }
    }

    public VideoBackupConfig getVideoBackupConfig(int configId) {
        VideoBackupConfigInfo videoBackupConfig = archiveMapper.getVideoBackupConfig(configId);

        return videoBackupConfig.getVideoBackupConfig();
    }

    public List<VideoBackupConfigInfo> getAllVideoBackupConfigs() {
        return archiveMapper.getAllVideoBackupConfig();
    }

    public int addNewVideoBackupConfig(VideoBackupConfig config, String desc) throws IllegalConfigException {
        if (!config.check()) {
            throw new IllegalConfigException(config);
        }
        VideoBackupConfigInfo videoBackupConfigInfo = new VideoBackupConfigInfo(desc, config);
        archiveMapper.insertVideoBackupConfig(videoBackupConfigInfo);
        videoConfigIds.add(videoBackupConfigInfo.getId());
        return videoBackupConfigInfo.getId();
    }

    public void updateVideoBackupConfig(VideoBackupConfigInfo info) {
        archiveMapper.updateVideoBackupConfig(info);
    }

    public void setVideoCfgIds(List<Integer> ids) throws ConfigListMismatchException {
        checkVideoIds(ids);
        this.videoConfigIds = ids;
    }

    public List<Integer> getVideoCfgIds() {
        return videoConfigIds;
    }

    public boolean isAHigherPriorityForVideo(int oldConfigId, int newConfigId) {
        int oldPriority = getVideoCfgPriority(oldConfigId);
        int newPriority = getVideoCfgPriority(newConfigId);
        return newPriority < oldPriority;
    }

    /**
     * 获取某视频备份配置的优先级
     *
     * @param configId 视频备份配置的ID
     * @return 优先级，越低越高
     */
    public int getVideoCfgPriority(int configId) {
        for (int i = 0; i < videoConfigIds.size(); i++) {
            if (configId == videoConfigIds.get(i)) {
                return i;
            }
        }
        throw new RuntimeException("configId:" + configId + " 没有对应的");
    }

    public List<FavoriteVideoInfo> getFavoriteVideoInfos(long favId) {
        return archiveMapper.getFavoriteVideoInfos(favId);
    }

    public void insertFavoriteVideo(long favoritesId, String bvid, long avid, long favTime) {
        archiveMapper.insertUserFavoriteVideo(favoritesId, bvid, avid, favTime);
    }

    /**
     * 在备份收藏夹发现视频失效时调用此方法
     */
    public void insertFailedVideoInfo(String bvid, long avid, String ownerName, long ownerMid, int collect, int play, int danmaku) {
        ArchiveVideoInfo info = new ArchiveVideoInfo();
        info.bvid = bvid;
        info.avid = avid;
        info.owner_name = ownerName;
        info.owner_mid = ownerMid;
        info.favorite = collect;
        info.view = play;
        info.danmaku = danmaku;
        info.pages = "[]";
        info.state = ArchiveVideoInfo.STATE_FAILED_AND_NO_BACKUP;
        info.title = "[在备份前已失效]";
        info.downloading = 0;
        info.config_id = 0;
        info.save_time = System.currentTimeMillis();
        info.community_update_time = System.currentTimeMillis();
        archiveMapper.insertArchiveVideoInfo(info);
    }

    public void deleteFavoriteVideo(long favId, String bvid) {
        archiveMapper.deleteFavoriteVideo(favId, bvid);
    }

    public List<HistoryVideoInfo> getNotFailedHistoryVideos(long uid) {
        return archiveMapper.getNotFailedHistoryVideos(uid);
    }

    public void updateHisVideoViewAt(long viewAt, String bvid) {
        archiveMapper.updateHisVideoViewAt(viewAt, bvid);
    }

    public void insertHistoryVideo(long uid, String bvid, long avid, long view_at) {
        archiveMapper.insertHistoryVideo(uid, bvid, avid, view_at);
    }

    public void deleteHistoryVideo(long uid, String bvid) {
        archiveMapper.deleteHistoryVideo(uid, bvid);
    }

    public static boolean checkVideoIsFailedFromCode(int respCode){
        return respCode == VideoInfo.VIDEO_FAILED_CODE || respCode == VideoInfo.VIDEO_FAILED_CODE_UP_DELETED;
    }
    public void updateVideoToFailed(String bvid,int respCode){
        assert respCode != 0;
        if(respCode == VideoInfo.VIDEO_FAILED_CODE){
            updateVideoStatus(bvid, ArchiveVideoInfo.STATE_FAILED);
        } else if (respCode == VideoInfo.VIDEO_FAILED_CODE_UP_DELETED){
            updateVideoStatus(bvid, ArchiveVideoInfo.STATE_FAILED_UP_DELETE);
        } else {
            throw new IllegalArgumentException("未知响应码："+respCode);
        }
    }

    public void updateVideoStatus(String bvid, int state) {
        archiveMapper.updateVideoStatus(state, bvid);
    }

    public void supplementaryFailedVideoInfo(String bvid, String title, String tag_name, String coverUrl) {

    }

    public long calculateVideosHardDiskUsage(String bvid) {
        long totalSize = 0;
        File videoBvidFile = new File(videosPath, bvid);
        if (videoBvidFile.exists()) {
            File[] files = videoBvidFile.listFiles();
            if (files == null) {
                return 0;
            }
            for (File file : files) {
                if (file.getName().startsWith("c_")) {
                    File videoFile = new File(file, "video.mp4");
                    if (videoFile.exists()) {
                        totalSize += videoFile.length();
                    }
                }
            }
        }
        return totalSize;
    }

    public boolean deleteVideoArchiveIfNotNotReferences(ArchiveVideoInfo archiveVideoInfo) throws IOException {
        String bvid = archiveVideoInfo.bvid;
        String title = archiveVideoInfo.title;
        if (!deleteVideoInfoIfNotReferences(bvid)) {
            System.out.printf("视频档案：%s(%s)还有别的项目引用（收藏夹、历史记录等），不执行删除%n", bvid, title);
            return false;
        }
        System.out.println("删除评论……");
        List<String> pendingDeletePictures = deleteComments(archiveVideoInfo.avid, 1);
        for (String pictureUrl : pendingDeletePictures) {
            File pictureFile = new File(commentPicturePath, MiscUtils.getEndPathForHttpUrl(pictureUrl));
            if (pictureFile.exists()) {
                FileUtil.deleteOneFile(pictureFile);
                System.out.println("评论图片" + pictureFile.getAbsolutePath() + "已删除！");
            }
        }
        System.out.println("删除视频、弹幕……");
        deleteVideoFilesByBvid(archiveVideoInfo.bvid);
        //如果只有本视频引用这个UP主头像，则删除
        String avatarUrl = archiveVideoInfo.owner_avatar_url;
        if (archiveMapper.getUpAvatarCount(avatarUrl) == 1){
            File avatarFile = new File(upAvatarPath,MiscUtils.getEndPathForHttpUrl(avatarUrl));
            FileUtil.deleteOneFile(avatarFile);
            System.out.println("已删除UP主头像文件："+avatarFile.getAbsolutePath());
        }
        System.out.println("视频删除完成！");
        return true;
    }

    /**
     * 删除评论区oid下的所有评论
     *
     * @param oid 评论区oid
     * @return 待删除的评论图片
     */
    public List<String> deleteComments(long oid, int type) throws IOException {
        List<String> pendingDeleteCommentPictures = new ArrayList<>();
        //创建一个Set集合（去重头像URL），然后将待删除评论的头像URL插入
        Set<String> avatarUrls = new HashSet<>();
        Cursor<ArchiveComment> comments = commentMapper.getComments(oid, type);
        for (ArchiveComment comment : comments) {
            avatarUrls.add(comment.avatar_url);
            //添加待删除的评论图片
            String[] allPictureUrl = comment.getAllPictureUrl();
            if (allPictureUrl != null) {
                pendingDeleteCommentPictures.addAll(List.of(allPictureUrl));
            }
        }
        //正式删除评论
        int deleteCount = commentMapper.deleteComments(oid, type);
        //删除头像（如果没有其他评论引用）
        for (String avatarUrl : avatarUrls) {
            commentMapper.getCommentAvatarCount(avatarUrl);
            if (commentMapper.getCommentAvatarCount(avatarUrl) == 0) {
                //删除头像，请注意，头像表中的name是URL末尾的文件名，评论表中的是完整的URL，不要搞错！
                commentMapper.deleteAvatar(MiscUtils.getEndPathForHttpUrl(avatarUrl));
            }
        }
        comments.close();
        System.out.println("已删除 "+deleteCount+" 个评论");
        return pendingDeleteCommentPictures;
    }

    public void deleteVideoFilesByBvid(String bvid) {
        File videoFileDir = new File(videosPath, bvid);
        if (videoFileDir.exists()) {
            FileUtil.deleteDirs(videoFileDir);
        }
    }

    public static class ConfigListMismatchException extends Exception {
        public ConfigListMismatchException(String message) {
            super(message);
        }
    }

}
