package top.weixiansen574.bilibiliArchive.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MiscUtils {
    public static final SimpleDateFormat cnSdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.CHINA);
    public static String getEndPathForHttpUrl(String url){
        String[] split = url.split("/");
        return split[split.length - 1];
    }

    public static void downloadToFile(OkHttpClient client, File file, String url, String referer) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url);
        if (referer != null){
            builder.addHeader("Referer",referer);
        }
        Request request = builder.build();
        int retryLimit = 15;
        for (int i = 1; i <= retryLimit; i++) {
            try {
                ResponseBody body = client.newCall(request).execute().body();
                OkHttpUtil.responseBodyNotNull(body);
                FileUtil.outputToFile(body.byteStream(),file);
                body.close();
                break;
            } catch (IOException e) {
                System.err.printf("下载链接 %s 时发发生异常：%s 正在重试[%d/%d]……%n",url,e.getMessage(),i,retryLimit);
                e.printStackTrace();
                if (i == 15){
                    throw e;
                }
            }
        }
    }

    public static byte[] downloadToBytes(OkHttpClient client, String url, String referer) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(url);
        if (referer != null){
            builder.addHeader("Referer",referer);
        }
        Request request = builder.build();
        int retryLimit = 15;
        for (int i = 1; i <= retryLimit; i++) {
            try {
                ResponseBody body = client.newCall(request).execute().body();
                OkHttpUtil.responseBodyNotNull(body);
                byte[] bytes = body.bytes();
                body.close();
                return bytes;
            } catch (IOException e) {
                System.err.printf("下载链接 %s 时发发生异常：%s 正在重试[%d/%d]……%n",url,e.getMessage(),i,retryLimit);
                e.printStackTrace();
                if (i == 15){
                    throw e;
                }
            }
        }
        return null;
    }

    public static String omit(String text,int length){
        if (text.length() > length){
            return text.substring(0,length) + "……";
        } else {
            return text;
        }
    }
}
