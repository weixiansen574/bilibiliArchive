package top.weixiansen574.bilibiliArchive.config;

import top.weixiansen574.bilibiliArchive.config.user.FavoritesBackupConfig;
import top.weixiansen574.bilibiliArchive.config.user.HistoryBackupConfig;
import top.weixiansen574.bilibiliArchive.config.user.UploaderBackupConfig;

import java.util.ArrayList;
import java.util.List;

public class UserBackupConfig {
    public HistoryBackupConfig historyBackupConfig;
    public List<FavoritesBackupConfig> favoritesBackupConfigs;
    public List<UploaderBackupConfig> uploaderBackupConfigs;

    public static UserBackupConfig createNew(){
        UserBackupConfig config = new UserBackupConfig();
        //config.historyBackupConfig = HistoryBackupConfig.createNew();
        config.favoritesBackupConfigs = new ArrayList<>();
        config.uploaderBackupConfigs = new ArrayList<>();
        return config;
    }


}
