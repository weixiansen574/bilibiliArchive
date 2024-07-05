package top.weixiansen574.bilibiliArchive.task.contentupdate;

public class VideoUpdatePlan extends ContentUpdatePlan {
    public String bvid;
    public Long avid;
    public String title;

    public Integer update_video_and_danmaku;

    public Integer video_quality;
    public Integer video_codec_id;

    @Override
    public String toString() {
        return "VideoUpdatePlan{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", uid=" + uid +
                ", update_comment=" + update_comment +
                ", comment_root_limit=" + comment_root_limit +
                ", comment_sort_type=" + comment_sort_type +
                ", comment_time_limit=" + comment_time_limit +
                ", comment_reply_use_preview=" + comment_reply_use_preview +
                ", do_not_update_comment_when_a_certain_quantity=" + do_not_update_comment_when_a_certain_quantity +
                ", bvid='" + bvid + '\'' +
                ", avid=" + avid +
                ", title='" + title + '\'' +
                ", update_video_and_danmaku=" + update_video_and_danmaku +
                ", video_quality=" + video_quality +
                ", video_codec_id=" + video_codec_id +
                '}';
    }
}
