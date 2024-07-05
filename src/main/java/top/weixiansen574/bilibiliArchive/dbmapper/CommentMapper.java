package top.weixiansen574.bilibiliArchive.dbmapper;


import org.apache.ibatis.annotations.*;
import org.apache.ibatis.cursor.Cursor;
import top.weixiansen574.bilibiliArchive.archive.ArchiveComment;
import top.weixiansen574.bilibiliArchive.archive.CMAvatar;

import java.util.List;

public interface CommentMapper {

    @Select("SELECT COUNT(1) FROM comments WHERE rpid = #{rpid}")
    boolean checkCommentIsExists(long rpid);

    @Select("SELECT COUNT(1) FROM avatars WHERE name = #{name}")
    boolean checkAvatarIsExists(@Param("name") String name);

    @Insert("INSERT INTO avatars (name, data) VALUES (#{name}, #{data})")
    void insertAvatar(CMAvatar avatar);

    @Insert("""
    INSERT INTO comments (
        rpid, oid, type, mid, root, parent, uname,
        current_level, location, message, like, ctime,
        pictures, avatar_url
    ) VALUES (
        #{rpid}, #{oid}, #{type}, #{mid}, #{root}, #{parent}, #{uname},
        #{current_level}, #{location}, #{message}, #{like}, #{ctime},
        #{pictures}, #{avatar_url}
    )
    """)
    void insertComment(ArchiveComment comment);

    /**
     * 获取所有根评论（root=0,不是回复的）
     */
    @Select("SELECT * FROM comments WHERE oid = #{oid} AND type = #{type} AND root = 0")
    List<ArchiveComment> getRootCommentList(@Param("oid") long oid,@Param("type") int type);

    /**
     * 获取oid下所有评论，包括回复的（删除时调用）
     */
    @Select("SELECT * FROM comments WHERE oid = #{oid} AND type = #{type}")
    Cursor<ArchiveComment> getComments(@Param("oid")long oid,@Param("type") int type);

    @Select("SELECT COUNT(*) FROM comments WHERE oid = #{oid} AND type = #{type}")
    int getCommentCountForOid(@Param("oid") long oid,@Param("type") int type);

    @Select("SELECT COUNT(*) FROM comments WHERE avatar_url = #{avatarUrl}")
    int getCommentAvatarCount(@Param("avatarUrl") String avatarUrl);

    @Delete("DELETE FROM avatars WHERE name = #{name}")
    void deleteAvatar(@Param("name") String fileName);

    @Delete("DELETE FROM comments WHERE oid = #{oid} AND type = ${type}")
    int deleteComments(@Param("oid") long oid, @Param("type") int type);

    @Update("UPDATE comments SET like = #{like} WHERE rpid = #{rpid}")
    void updateCommentLike(@Param("like") int like,@Param("rpid") long rpid);

    @Select("SELECT * FROM comments WHERE oid = #{oid} AND type = #{type} ORDER BY ctime DESC LIMIT 1")
    ArchiveComment getLatestPostedComment(@Param("oid") long oid,@Param("type") int type);


}
