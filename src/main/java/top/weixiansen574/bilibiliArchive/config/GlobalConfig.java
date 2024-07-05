package top.weixiansen574.bilibiliArchive.config;

import java.util.ArrayList;
import java.util.List;

public class GlobalConfig {
    public Long intervalOfLoop;
    public List<Integer> videoConfigIds;

    public static GlobalConfig createNew(){
        GlobalConfig config = new GlobalConfig();
        config.intervalOfLoop = 3600000L;//一小时
        config.videoConfigIds = new ArrayList<>();
        return config;
    }


}
