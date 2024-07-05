package top.weixiansen574.bilibiliArchive.biliApis;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;

public interface BiliApiService {
    public static final int COMMENT_SORT_BY_TIME = 1;
    @GET("/x/web-interface/view")
    Call<GeneralResponse<VideoInfo>> getVideoInfoByBvid(@Query("bvid") String bvid);
    @GET("/x/space/wbi/acc/info")
    Call<GeneralResponse<UserInfo>> getUserInfo(@Query("mid") long mid);
    @GET("/x/player/playurl")
    Call<GeneralResponse<VideoPlayUrlInfo>> getVideoPlayUrlInfoByAvid(@Query("avid") long avid,@Query("cid") long cid);
    @GET("/x/player/playurl")
    Call<GeneralResponse<VideoPlayUrlInfo>> getVideoPlayUrlInfoByBvid(@Query("bvid") String avid,@Query("cid") long cid,@Query("fnval") int fnval);

    /**
     * 获取评论区根页
     * @param sort 排序方式 时间戳降序：0  点赞降序：1  回复数降序：2
     * @param pn 页码，从1开始
     * @param oid 评论区oid
     * @param type 评论区type 1:视频 ……
     */
    @GET("/x/v2/reply")
    Call<GeneralResponse<CommentPage>> getCommentPage(@Query("sort") int sort,@Query("pn") int pn,@Query("oid") long oid,@Query("type") int type);
    @GET("/x/v2/reply/reply")
    Call<GeneralResponse<CommentReplyPage>> getCommentReplyPage(@Query("root") long root, @Query("oid") long oid, @Query("pn") int pn, @Query("type") int type);
    @GET("/x/v3/fav/folder/created/list-all")
    Call<GeneralResponse<FavoritesList>> getFavoritesList(@Query("up_mid") long mid);
    @GET("/x/v3/fav/resource/ids?platform=web")
    Call<GeneralResponse<List<FavoriteVideo>>> getFavoriteVideos(@Query("media_id") long fid);
    @GET("/x/v3/fav/resource/list")
    Call<GeneralResponse<DetailedFavoriteVideoInfosPage>> getFavoriteVideosInDetailed(@Query("media_id") long fid,@Query("pn") int pn,@Query("ps") int ps,@Query("type") int type);
    @GET("/x/web-interface/history/cursor")
    Call<GeneralResponse<HistoriesPage>> getLatestHistories(@Query("ps") int ps, @Query("type") String type, @Query("business") String business);
    @GET("/x/web-interface/history/cursor?type=archive&business=archive")
    Call<GeneralResponse<HistoriesPage>> getLatestVideoHistories(@Query("ps") int ps);
    @GET("/x/web-interface/history/cursor")
    Call<GeneralResponse<HistoriesPage>> getHistories(@Query("ps") int ps, @Query("type") String type, @Query("business") String business, @Query("max") long max, @Query("view_at") long view_at);
    @GET("/x/web-interface/history/cursor?type=archive&business=archive")
    Call<GeneralResponse<HistoriesPage>> getVideoHistories(@Query("ps") int ps, @Query("max") long max, @Query("view_at") long view_at);
    @GET("//member.bilibili.com//x2/creative/h5/calendar/event?ts=0")
    Call<GeneralResponse<CalendarEvent>> getCalendarEvents();
}
