package top.weixiansen574.bilibiliArchive.downloaders;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;
import top.weixiansen574.bilibiliArchive.ArchiveManger;
import top.weixiansen574.bilibiliArchive.archive.Danmaku;
import top.weixiansen574.bilibiliArchive.archive.DownloadedVideoPage;
import top.weixiansen574.bilibiliArchive.biliApis.BiliApiService;
import top.weixiansen574.bilibiliArchive.biliApis.BiliBiliApiException;
import top.weixiansen574.bilibiliArchive.biliApis.VideoInfo;
import top.weixiansen574.bilibiliArchive.biliApis.VideoPlayUrlInfo;
import top.weixiansen574.bilibiliArchive.util.FileUtil;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VideoDownloader extends CommentDownloader {

    public VideoDownloader(OkHttpClient httpClient, BiliApiService biliApiService, ArchiveManger archiveManger) {
        super(httpClient, biliApiService, archiveManger);
    }
    /**
     * 下载视频，BV号下的所有分p视频（纯视频流&音频流，最后合并）
     * @param oldDownloadedVideoPages 以前下载的视频分p列表，如果有相符且画质编码一致的则跳过下载,节省时间流量
     */
    public List<DownloadedVideoPage> downloadOrUpdateVideo(
            VideoInfo videoInfo, @Nullable List<DownloadedVideoPage> oldDownloadedVideoPages,
            int videoMaxQn, int videoCodec, int audioMaxBandwidth, boolean isVip) throws IOException, BiliBiliApiException {

        String bvid = videoInfo.bvid;
        List<VideoInfo.Page> pages = videoInfo.pages;
        List<DownloadedVideoPage> downloadedVideoPages = new ArrayList<>();
        if (videoInfo.pages == null) {
            return downloadedVideoPages;
        }
        File bvidCacheDir = createBvidCacheDir(bvid);
        for (VideoInfo.Page page : pages) {
            long cid = page.cid;
            VideoPlayUrlInfo videoPlayUrlInfo = OkHttpUtil
                    .getData(biliApiService.getVideoPlayUrlInfoByBvid(bvid, cid, 4048));
            VideoPlayUrlInfo.Video dashVideo = videoPlayUrlInfo.getEligibleDashVideo(videoMaxQn, videoCodec, isVip);
            if (dashVideo == null){
                throw new IOException("画质列表与视频链接列表不匹配，请检查cookie是否失效？");
            }
            System.out.println("下载cid=" + cid + "的视频……");

            if (checkVideoPageNeedDownload(oldDownloadedVideoPages,cid,dashVideo)) {
                File videoCacheFile = downloadVideoStream(dashVideo, bvid, cid);
                File audioCacheFile = downloadAudioStream(videoPlayUrlInfo.getEligibleDashAudio(audioMaxBandwidth), bvid, cid);
                if (audioCacheFile != null){
                    archiveManger.mergeAndSaveVideoFile(bvid,cid,videoCacheFile,audioCacheFile);
                } else {
                    archiveManger.saveNoAudioVideoFile(bvid,cid,videoCacheFile);
                }
                System.out.println("单p视频下载完成");
            } else {
                System.out.println("视频已存在且画质与将要下载的一致，跳过下载");
            }
            downloadedVideoPages.add(new DownloadedVideoPage(page,dashVideo.codecid,dashVideo.id,
                    dashVideo.width,dashVideo.height));
        }
        FileUtil.deleteDirs(bvidCacheDir);
        return downloadedVideoPages;
    }


    public void downloadCover(String coverUrl, String bvid) throws IOException {
        System.out.println("下载封面……");
        Request request = createDownloadRequestFromTheVideo(coverUrl, bvid);
        File coverFile = archiveManger.newCoverFile(bvid);
        downloadContentAndRetry(request, coverFile, 15);
        System.out.println("封面下载完成");
    }

    /**
     * 下载或更新已存在的弹幕，如果是优先级重载的，那么就当做更新啦，更新不会删除旧的弹幕
     * @param videoInfo
     * @return
     */
    public void downloadOrUpdateDanmaku(VideoInfo videoInfo) throws IOException {
        String bvid = videoInfo.bvid;
        for (VideoInfo.Page page : videoInfo.pages) {
            long cid = page.cid;
            System.out.println("下载cid:"+cid+" 的弹幕");
            downloadDMByProtobuf(archiveManger.newDanmakuFile(bvid,cid),cid,bvid,page.duration);
            System.out.println("弹幕下载完成");
        }
    }

    private void downloadDMByProtobuf(File danmakuFile, long cid, String bvid, int videoLength) throws IOException{

        //下载弹幕并存到内存
        List<Danmaku> danmakuList = new ArrayList<>();
        for (int segment_index = 1; segment_index <= videoLength / 360 + 1; segment_index++) {
            byte[] bytes = downloadContentAndRetry(createDownloadRequestFromTheVideo(
                    "https://api.bilibili.com/x/v2/dm/web/seg.so?type=1&oid=" +
                    cid + "&segment_index=" + segment_index, bvid),15);
            danmakuList.addAll(Danmaku.parseFromProtobuf(bytes));
        }

        List<Danmaku> oldDanmakuList = null;
        if (danmakuFile.exists()) {
            try {
                oldDanmakuList = Danmaku.paresFromXML(new FileInputStream(danmakuFile));
            } catch (SAXException | ParserConfigurationException e) {
                e.printStackTrace();
                File newFile = new File(danmakuFile.getParent(), "danmaku_backup_" +
                        System.currentTimeMillis() + ".xml");
                FileUtil.copyFile(danmakuFile,newFile);
                FileUtil.deleteOneFile(danmakuFile);
                System.out.println("以前的弹幕文件损坏了，更新弹幕将重建文件，旧的弹幕文件备份至："+newFile);
            }
        }

        //保存到文件或者更新
        try {
            if (oldDanmakuList != null) {
                danmakuList = Danmaku.merge(danmakuList,oldDanmakuList);
            }
            Danmaku.toXML(danmakuList,cid,new FileOutputStream(danmakuFile));
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException(e);
        }

    }

    protected File downloadVideoStream(VideoPlayUrlInfo.Video dashVideo, String bvid, long cid) throws IOException {

        for (int i = 0; i < dashVideo.backupUrl.size(); i++) {
            try {
                return downloadVideoStream(dashVideo.backupUrl.get(i), bvid, cid);
            } catch (IOException ignored) {
            }
        }
        //最后一个还是失败就扔异常
        return downloadVideoStream(dashVideo.baseUrl, bvid, cid);
    }

    protected File downloadAudioStream(VideoPlayUrlInfo.Audio dashAudio, String bvid, long cid) throws IOException {
        //一些特殊的视频没有音频流
        if (dashAudio == null){
            return null;
        }
        for (int i = 0; i < dashAudio.backupUrl.size(); i++) {
            try {
                return downloadAudioStream(dashAudio.backupUrl.get(i), bvid, cid);
            } catch (IOException ignored) {
            }
        }
        return downloadAudioStream(dashAudio.baseUrl, bvid, cid);
    }

    protected boolean checkVideoPageNeedDownload(List<DownloadedVideoPage> pages, long cid, VideoPlayUrlInfo.Video dashVideo) {
        if (pages == null) {
            return true;
        }
        for (DownloadedVideoPage page : pages) {
            if (page.cid == cid) {
                return page.qn != dashVideo.id || page.codecId != dashVideo.codecid;
            }
        }
        return true;
    }

    protected File createBvidCacheDir(String bvid) throws IOException {
        return FileUtil.getOrCreateDir(cachePath, bvid);
    }


    protected File downloadVideoStream(String url, String bvid, long cid) throws IOException {
        Request request = createDownloadRequestFromTheVideo(url, bvid);
        File videoStreamFile = new File(cachePath, bvid + "/" + "video_" + cid + ".m4s");
        downloadContentAndRetry(request, videoStreamFile, 3);
        return videoStreamFile;
    }

    protected File downloadAudioStream(String url, String bvid, long cid) throws IOException {
        Request request = createDownloadRequestFromTheVideo(url, bvid);
        File audioStreamFile = new File(cachePath, bvid + "/" + "audio_" + cid + ".m4s");
        downloadContentAndRetry(request, audioStreamFile, 3);
        return audioStreamFile;
    }

    protected static Request createDownloadRequestFromTheVideo(String url, String refBvid) {
        return createDownloadRequest(url, "https://www.bilibili.com/video/" + refBvid);
    }
}
