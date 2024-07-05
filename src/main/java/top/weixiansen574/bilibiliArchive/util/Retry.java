package top.weixiansen574.bilibiliArchive.util;

import java.io.IOException;

public interface Retry<T> {
    T run() throws IOException;

    static <T> T loop(Retry<T> retry,int count) throws IOException {
        for (int i = 0; i < count; i++) {
            try {
                return retry.run();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        return retry.run();
    }
}
