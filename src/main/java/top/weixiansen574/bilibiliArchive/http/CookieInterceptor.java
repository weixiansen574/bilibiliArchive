package top.weixiansen574.bilibiliArchive.http;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public record CookieInterceptor(String cookie) implements Interceptor {

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String url = originalRequest.url().toString();
        url = url.replaceAll("^http://", "https://");//强制启用https，避免被校园网偷窥佬抓包（其实我还为了避免抓ua导致校园网被踢）
        if (originalRequest.header("Cookie") == null) {
            Request newRequest = originalRequest.newBuilder()
                    .url(url)
                    .addHeader("Cookie", cookie)
                    .build();
            return chain.proceed(newRequest);
        }
        return chain.proceed(originalRequest);
    }

}
