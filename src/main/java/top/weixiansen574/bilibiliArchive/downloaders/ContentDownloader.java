package top.weixiansen574.bilibiliArchive.downloaders;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.weixiansen574.bilibiliArchive.ArchiveManger;
import top.weixiansen574.bilibiliArchive.biliApis.BiliApiService;
import top.weixiansen574.bilibiliArchive.http.ResponseNotSuccessfulException;
import top.weixiansen574.bilibiliArchive.util.FileUtil;
import top.weixiansen574.bilibiliArchive.util.MiscUtils;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import java.io.File;
import java.io.IOException;

public abstract class ContentDownloader {
    public File cachePath;
    public OkHttpClient httpClient;
    public BiliApiService biliApiService;
    public ArchiveManger archiveManger;

    public ContentDownloader(OkHttpClient httpClient, BiliApiService biliApiService, ArchiveManger archiveManger) {
        this.cachePath = archiveManger.getCachePath();
        this.httpClient = httpClient;
        this.biliApiService = biliApiService;
        this.archiveManger = archiveManger;
    }

    public void downloadUpAvatarIfNotExists(String avatarUrl) throws IOException {
        Request request = createDownloadRequest(avatarUrl);
        File avatarFile = archiveManger.newUploaderAvatarFile(MiscUtils.getEndPathForHttpUrl(avatarUrl));
        if (!avatarFile.exists()){
            try {
                downloadContentAndRetry(request,avatarFile,15);
            } catch (IOException e){
                //下载失败后移除文件，以免坏文件判断成已下载就不覆盖了
                FileUtil.deleteOneFile(avatarFile);
                throw e;
            }
        }
    }

    public void downloadContentAndRetry(Request request,File file,int tryCount) throws IOException {
        for (int i = 1; i <= tryCount; i++) {
            try {
                downloadContent(request,file);
                return;
            } catch (IOException e){
                //仅重试网络环境引起的问题，状态码不成功不重试
                if (e instanceof ResponseNotSuccessfulException){
                    throw e;
                }
                System.err.printf("下载：%s 的时候发生 %s 异常，正在重试(%d/%d)……",
                        request.url(),e.getMessage(),i,tryCount);
            }
        }
        downloadContent(request,file);
    }

    public byte[] downloadContentAndRetry(Request request,int tryCount) throws IOException {
        for (int i = 1; i <= tryCount; i++) {
            try {
                return downloadContent(request);
            } catch (IOException e){
                //仅重试网络环境引起的问题，状态码不成功不重试
                if (e instanceof ResponseNotSuccessfulException){
                    throw e;
                }
                System.err.printf("下载：%s 的时候发生 %s 异常，正在重试(%d/%d)……",
                        request.url(),e.getMessage(),i,tryCount);
            }
        }
        return downloadContent(request);
    }

    public void downloadContent(Request request,File file) throws IOException{
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()){
            throw new ResponseNotSuccessfulException(response);
        }
        ResponseBody body = response.body();
        OkHttpUtil.responseBodyNotNull(body);
        try {
            FileUtil.outputToFile(body.byteStream(),file);
            body.close();
        } catch (IOException e){
            //如果下载失败则删除文件，避免下载再来发现文件已下载然后跳过下载
            FileUtil.deleteOneFile(file);
            throw e;
        }
    }

    public byte[] downloadContent(Request request) throws IOException{
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()){
            throw new ResponseNotSuccessfulException(response);
        }
        ResponseBody body = response.body();
        OkHttpUtil.responseBodyNotNull(body);
        byte[] bytes = body.bytes();
        body.close();
        return bytes;
    }


    public static Request createDownloadRequest(String url){
        return createDownloadRequest(url,"https://www.bilibili.com/");
    }

    public static Request createDownloadRequest(String url, String referer) {
        return new Request.Builder()
                .url(url)
                .addHeader("Referer", referer)
                .build();
    }
}
