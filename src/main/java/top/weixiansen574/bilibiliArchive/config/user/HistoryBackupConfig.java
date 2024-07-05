package top.weixiansen574.bilibiliArchive.config.user;

import com.alibaba.fastjson.annotation.JSONField;
import top.weixiansen574.bilibiliArchive.config.BlackListUser;

import java.util.ArrayList;
import java.util.List;

public class HistoryBackupConfig extends BaseBackupConfig {
    public int videoBackupConfigId;
    public static final String DELETE_METHOD_ITEM_QUANTITY = "itemQuantity";
    public static final String DELETE_METHOD_DAYS = "days";
    public static final String DELETE_METHOD_DISK_USAGE = "diskUsage";
    public int deleteByDays;
    public long deleteByDiskUsage;
    public int deleteByItemQuantity;
    public String deleteMethod;
    public long doNotDownloadVideoReleasedAFewDaysAgo = -1;//-1代表无限大，下载没有发布时间限制
    public List<BlackListUser> upBlackList;

    @JSONField(serialize = false)
    public long getDeleteByDaysTotalTimeSec(){
        return ((long) deleteByDays) * 24 * 60 * 60;
    }

    public void addToUpBlackList(BlackListUser user){
        if (upBlackList == null){
            upBlackList = new ArrayList<>();
        }
        upBlackList.add(user);
    }

    public static HistoryBackupConfig createNew(){
        HistoryBackupConfig config = new HistoryBackupConfig();
        return config;
    }



}
