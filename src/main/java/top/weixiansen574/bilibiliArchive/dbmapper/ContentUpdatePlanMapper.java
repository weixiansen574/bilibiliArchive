package top.weixiansen574.bilibiliArchive.dbmapper;

import org.apache.ibatis.annotations.*;
import top.weixiansen574.bilibiliArchive.task.contentupdate.VideoUpdatePlan;

public interface ContentUpdatePlanMapper {

    @Insert("""
        INSERT INTO video_update_plans (
            timestamp, uid, bvid, avid, title,
            update_comment, update_video_and_danmaku, comment_root_limit,
            comment_sort_type, comment_time_limit, comment_reply_use_preview,
            do_not_update_comment_when_a_certain_quantity,
            video_quality, video_codec_id
        ) VALUES (
            #{timestamp}, #{uid}, #{bvid}, #{avid}, #{title},
            #{update_comment}, #{update_video_and_danmaku}, #{comment_root_limit},
            #{comment_sort_type}, #{comment_time_limit}, #{comment_reply_use_preview},
            #{do_not_update_comment_when_a_certain_quantity},
            #{video_quality}, #{video_codec_id}
        )
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVideoUpdatePlan(VideoUpdatePlan videoUpdatePlan);

    @Select("SELECT * FROM video_update_plans ORDER BY timestamp ASC LIMIT 1")
    VideoUpdatePlan getEarliestVideoUpdatePlan();

    @Select("DELETE FROM video_update_plans WHERE bvid = #{bvid}")
    void deleteVideoUpdatePlanByBvid(@Param("bvid") String bvid);

    @Select("DELETE FROM video_update_plans WHERE id = #{id}")
    void deleteVideoUpdatePlanById(@Param("id") int id);
}
