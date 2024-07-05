package top.weixiansen574.bilibiliArchive.archive;

import com.alibaba.fastjson2.JSON;
import top.weixiansen574.bilibiliArchive.VideoPageVersion;
import top.weixiansen574.bilibiliArchive.biliApis.VideoInfo;

import java.util.List;

public class ArchiveVideoInfo {
    public static final int STATE_NORMAL = 0;
    public static final int STATE_FAILED = 404;
    public static final int STATE_FAILED_UP_DELETE = 4049;//对应收藏夹里“UP主删除”的attr值：9
    public static final int STATE_BACKUP_IS_NOT_SUPPORTED = -403;//不支持备份的视频，比如充电专属视频
    public static final int STATE_FAILED_AND_NO_BACKUP = -404;
    public static final int DOWNLOAD_STATE_OK = 0;
    public static final int DOWNLOAD_STATE_DOWNLOADING = 1;
    public static final int DOWNLOAD_STATE_FAILED = 2;

    public String bvid;

    public long avid;

    public String title;

    public String desc;

    public long owner_mid;

    public String owner_name;

    public long view;

    public int danmaku;

    public int favorite;

    public int coin;

    public long like;

    public int share;

    public int reply;

    public String tname;

    public long ctime;

    public String cover_url;

    public String pages;

    public long save_time;

    public int state;

    public int downloading;

    public long community_update_time;

    public int config_id;

    public String owner_avatar_url;


    @Override
    public String toString() {
        return "ArchiveVideoInfo{" +
                "bvid='" + bvid + '\'' +
                ", avid=" + avid +
                ", title='" + title + '\'' +
                ", desc='" + desc + '\'' +
                ", owner_mid=" + owner_mid +
                ", owner_name='" + owner_name + '\'' +
                ", view=" + view +
                ", danmaku=" + danmaku +
                ", favorite=" + favorite +
                ", coin=" + coin +
                ", like=" + like +
                ", share=" + share +
                ", reply=" + reply +
                ", tname='" + tname + '\'' +
                ", ctime=" + ctime +
                ", cover_url='" + cover_url + '\'' +
                ", pages='" + pages + '\'' +
                ", save_time=" + save_time +
                ", state=" + state +
                ", downloading=" + downloading +
                ", community_update_time=" + community_update_time +
                ", config_id=" + config_id +
                ", owner_avatar_url='" + owner_avatar_url + '\'' +
                '}';
    }

    public ArchiveVideoInfo() {
    }


    private ArchiveVideoInfo(VideoInfo videoInfo, List<VideoPageVersion> pageVersions) {
        this.bvid = videoInfo.bvid;
        this.avid = videoInfo.aid;
        this.title = videoInfo.title;
        this.desc = videoInfo.desc;
        this.owner_mid = videoInfo.owner.mid;
        this.owner_name = videoInfo.owner.name;
        this.view = videoInfo.stat.view;
        this.danmaku = videoInfo.stat.danmaku;
        this.favorite = videoInfo.stat.favorite;
        this.coin = videoInfo.stat.coin;
        this.like = videoInfo.stat.like;
        this.share = videoInfo.stat.share;
        this.reply = videoInfo.stat.reply;
        this.tname = videoInfo.tname;
        this.ctime = videoInfo.ctime;
        this.cover_url = videoInfo.pic;
        setPageVersionsObject(pageVersions);
    }

    public ArchiveVideoInfo
            (VideoInfo videoInfo, List<VideoPageVersion> pageVersions, int state, long save_time,
             int config_id, long community_update_time, int downloading) {
        this(videoInfo, pageVersions);
        this.save_time = save_time;
        this.state = state;
        this.config_id = config_id;
        this.downloading = downloading;
        this.community_update_time = community_update_time;
        this.owner_avatar_url = videoInfo.owner.face;
    }

    public void setPageVersionsObject(List<VideoPageVersion> pageVersions){
        this.pages = JSON.toJSONString(pageVersions);
    }

    public List<VideoPageVersion> getPageVersionsObject(){
        return JSON.parseArray(pages, VideoPageVersion.class);
    }

    public boolean isFailed() {
        return state != STATE_NORMAL;
    }

    public static void addToLastIfInconsistency(List<VideoPageVersion> pageVersions,List<DownloadedVideoPage> pages){
        //直接为空
        if (pageVersions.isEmpty()){
            pageVersions.add(new VideoPageVersion(System.currentTimeMillis(),pages));
            return;
        }

        VideoPageVersion lastVideoPageVersion = pageVersions.get(pageVersions.size() - 1);

        //数量不一致
        if (lastVideoPageVersion.pages.size() != pages.size()){
            pageVersions.add(new VideoPageVersion(System.currentTimeMillis(),pages));
            return;
        }
        //cid编号不一致
        for (int i = 0; i < lastVideoPageVersion.pages.size(); i++) {
            DownloadedVideoPage lPage = lastVideoPageVersion.pages.get(i);
            DownloadedVideoPage nPage = pages.get(i);
            if (lPage.page != nPage.page){
                throw new IllegalArgumentException("两列表中的page顺序不一致！");
            }
            if (lPage.cid != nPage.cid){
                pageVersions.add(new VideoPageVersion(System.currentTimeMillis(),pages));
                return;
            }
        }
    }

}
