package top.weixiansen574.bilibiliArchive.dbmapper;


import org.apache.ibatis.annotations.*;
import top.weixiansen574.bilibiliArchive.archive.*;

import java.sql.SQLException;
import java.util.List;

public interface ArchiveMapper {

    @Insert("""
            INSERT INTO video_infos (
                bvid, avid, title, desc, owner_mid, owner_name, owner_avatar_url,
                view, danmaku, favorite, coin, like, share, reply, tname, ctime,
                cover_url, pages, state, downloading, save_time, community_update_time,
                config_id
            ) VALUES (
                #{bvid}, #{avid}, #{title}, #{desc}, #{owner_mid}, #{owner_name},
                #{owner_avatar_url}, #{view}, #{danmaku}, #{favorite}, #{coin},
                #{like}, #{share}, #{reply}, #{tname}, #{ctime}, #{cover_url}, #{pages},
                #{state}, #{downloading}, #{save_time}, #{community_update_time},
                #{config_id}
            )
            """)
    void insertArchiveVideoInfo(ArchiveVideoInfo videoInfo);

    //@Delete(table = "video_infos",where = "bvid = ?")
    @Delete("DELETE FROM video_infos WHERE bvid = #{bvid}")
    void deleteArchiveVideoInfo(String bvid) throws SQLException;

    @Delete("""
            DELETE FROM video_infos
            WHERE bvid = #{bvid}
            AND NOT EXISTS (
                SELECT 1 FROM user_favorite_videos WHERE bvid = #{bvid}
            )
            AND NOT EXISTS (
                SELECT 1 FROM user_history_videos WHERE bvid = #{bvid}
            )
            """)
    int deleteVideoInfoIfNotReferences(@Param("bvid") String bvid);

    //@Select(addition = "WHERE bvid = ?")
    @Select("SELECT * FROM video_infos WHERE bvid = #{bvid}")
    ArchiveVideoInfo getArchiveVideoInfo(@Param("bvid") String bvid);

    //@Update(table = "video_infos",set = "downloading = ?",where = "bvid = ?")
    @Update("UPDATE video_infos SET downloading = #{downloading} WHERE bvid = #{bvid}")
    int updateDownloadStatus(@Param("downloading") int status, @Param("bvid") String bvid);

    //@Update(table = "video_infos",set = "state = ?",where = "bvid = ?")
    @Update("UPDATE video_infos SET state = #{state} WHERE bvid = #{bvid}")
    int updateVideoStatus(@Param("state") int status, @Param("bvid") String bvid);

    //@Update(table = "video_infos",set = "comment_download_config = ?",where = "bvid = ?")
    @Update("UPDATE video_infos SET comment_download_config = #{config} WHERE bvid = #{bvid}")
    int updateVideoCommentDownloadConfig(@Param("config") String commentDownloadConfigJson, @Param("bvid") String bvid);

    //@Update(table = "video_infos",set = "config_id = ?",where = "bvid = ?")
    @Update("UPDATE video_infos SET config_id = #{config_id} WHERE bvid = #{bvid}")
    int updateVideoArchiveConfigId(@Param("config_id") int configId, @Param("bvid") String bvid);

    //@Update(table = "video_infos",set = "community_update_time = ?",where = "bvid = ?")
    @Update("UPDATE video_infos SET community_update_time = #{time} WHERE bvid = #{bvid}")
    int updateVideoCommunityUpdateTime(@Param("time") long time, @Param("bvid") String bvid);

    //@Update(table = "video_infos",set = "title = ?,tname = ?,cover_url = ?",where = "bvid = ?")
    @Update("UPDATE video_infos SET title = #{title},tname = #{tname},cover_url = #{cover_url} WHERE bvid = #{bvid}")
    int supplementaryFailedVideoInfo(@Param("title") String title, @Param("tname") String tagName,
                                     @Param("cover_url") String coverUrl, @Param("bvid") String bvid);

    @Select("SELECT * FROM video_infos")
    List<ArchiveVideoInfo> getAllArchiveInfos();

    @Select("SELECT COUNT(*) FROM video_infos where owner_avatar_url = #{url}")
    int getUpAvatarCount(@Param("url") String avatarUrl);


    //List<ArchiveVideoInfo> getAllArchiveVideoInfo();

    //@Update(table = "video_infos",set = "view = ?,danmaku = ?,favorite = ?,coin = ?,like = ?,share = ?,reply = ?",where = "bvid = ?")
    @Update("""
            UPDATE video_infos SET
            view = #{view},danmaku = #{danmaku},favorite = #{favorite},coin = #{coin},
            like = #{like},share = #{share},reply = #{reply},downloading = #{downloading},pages = #{pages}
            WHERE bvid = #{bvid}
            """)
    void updateArchiveVideoInfo(ArchiveVideoInfo videoInfo);

    @Select("SELECT * from video_backup_configs WHERE id = #{id}")
    VideoBackupConfigInfo getVideoBackupConfig(@Param("id") int id);

    @Select("SELECT * from video_backup_configs")
    List<VideoBackupConfigInfo> getAllVideoBackupConfig();

    @Insert("insert into video_backup_configs (desc,config) values(#{desc},#{config})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertVideoBackupConfig(VideoBackupConfigInfo backupConfigInfo);

    @Update("UPDATE video_backup_configs SET desc = #{desc},config = #{config} where id = #{id}")
    void updateVideoBackupConfig(VideoBackupConfigInfo info);


    /*
        @Update(table = "video_backup_configs",set = "config = ?",where = "id = ?")
        void updateVideoBackupConfig(String configJson,int id);

        @Update(table = "video_backup_configs",set = "desc = ?",where = "id = ?")
        void updateVideoBackupConfigDesc(String desc,int id);

        @SelectOneValue("select MAX(id) from video_backup_configs")
        long getNewInsertedVideoBackupConfigId();

        */


    @Insert("INSERT INTO user_history_videos (uid, bvid, avid, view_at) VALUES (#{uid}, #{bvid}, #{avid}, #{view_at})")
    int insertHistoryVideo(@Param("uid") long uid, @Param("bvid") String bvid, @Param("avid") long avid, @Param("view_at") long view_at);


    @Select("""
            SELECT uhv.uid,
                     uhv.view_at,
                     vi.*
            FROM (SELECT * FROM user_history_videos
                WHERE uid = #{uid}) AS uhv
            LEFT JOIN video_infos AS vi
                ON uhv.bvid = vi.bvid
            WHERE state = 0 ORDER BY view_at DESC;
            """)
    List<HistoryVideoInfo> getNotFailedHistoryVideos(@Param("uid") long uid);

    @Select("""
            SELECT uhv.uid, uhv.view_at, vi.*
            FROM (SELECT * FROM user_history_videos WHERE uid = ?) AS uhv
            LEFT JOIN video_infos AS vi ON uhv.bvid = vi.bvid;
            """)
    List<HistoryVideoInfo> getHistoryVideoInfo(long uid);

    //@Update(table = "user_history_videos", set = "view_at = ?", where = "bvid = ?")
    @Update("UPDATE user_history_videos SET view_at = #{viewAt} WHERE bvid = #{bvid}")
    int updateHisVideoViewAt(@Param("viewAt") long viewAt, @Param("bvid") String bvid);

    //@Delete(table = "user_history_videos", where = "uid = ? AND bvid = ?")
    @Delete("DELETE FROM user_history_videos WHERE uid = #{uid} AND bvid = #{bvid}")
    int deleteHistoryVideo(@Param("uid") long uid,@Param("bvid") String bvid);

    @Insert("INSERT INTO user_favorite_videos (favorites_id, bvid, avid, fav_time) VALUES " +
            "(#{favoritesId}, #{bvid}, #{avid}, #{favTime})")
    void insertUserFavoriteVideo(@Param("favoritesId") long favoritesId, @Param("bvid") String bvid,
                                 @Param("avid") long avid, @Param("favTime") long favTime);

    @Select("""
            SELECT uvf.favorites_id, uvf.fav_time, vi.*
            FROM (SELECT * FROM user_favorite_videos WHERE favorites_id = #{favId}) AS uvf
            LEFT JOIN video_infos AS vi ON uvf.bvid = vi.bvid;
            """)
    List<FavoriteVideoInfo> getFavoriteVideoInfos(@Param("favId") long favId);

    //@Delete(table = "user_favorite_videos",where = "favorites_id = ? AND bvid = ?")
    @Delete("DELETE FROM user_favorite_videos WHERE favorites_id = #{favId} AND bvid = #{bvid}")
    void deleteFavoriteVideo(@Param("favId") long favorites_id, @Param("bvid") String bvid);

}
