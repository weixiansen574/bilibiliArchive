package top.weixiansen574.bilibiliArchive.config;

import top.weixiansen574.bilibiliArchive.config.user.VideoContentUpdateConfig;

public class VideoBackupConfig {
    public VideoDownloadConfig video;
    public CommentDownloadConfig comment;
    public VideoContentUpdateConfig update;

    public boolean check(){
        if (video == null || comment == null) {
            return false;
        }

        return comment.check();
    }
}
