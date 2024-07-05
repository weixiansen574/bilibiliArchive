package top.weixiansen574.bilibiliArchive.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import top.weixiansen574.bilibiliArchive.biliApis.BiliBiliApiException;
import top.weixiansen574.bilibiliArchive.biliApis.GeneralResponse;

import java.io.IOException;

public class OkHttpUtil {

    public static <T> T getData(Call<GeneralResponse<T>> call) throws IOException, BiliBiliApiException {
        GeneralResponse<T> response = executeCall(call);
        return response.getDataNotNull("request info:"+call.request());
    }

    public static void responseBodyNotNull(ResponseBody responseBody) throws IOException {
        if (responseBody == null) {
            throw new IOException("response is null");
        }
    }

    /**
     * 执行请求，并返回响应体，如果未成功或响应体为null则抛出异常
     * @param call
     * @param <T>
     * @return
     * @throws IOException
     */
    public static <T> T executeCall(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (!response.isSuccessful()) {
            throw new IOException("response is not successful. Response: " + response);
        }
        T body = response.body();
        if (body == null) {
            throw new IOException("response body is null. raw response:" + response);
        }
        return body;
    }

    /*public static void responseObjNotNull(Object o) throws IOException {
        if (o == null){
            throw new IOException("response is null");
        }
    }*/
}
