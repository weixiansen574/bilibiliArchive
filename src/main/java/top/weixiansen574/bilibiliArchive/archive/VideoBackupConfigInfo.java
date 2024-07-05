package top.weixiansen574.bilibiliArchive.archive;

import com.alibaba.fastjson2.JSON;

import com.baomidou.mybatisplus.annotation.TableField;
import top.weixiansen574.bilibiliArchive.config.VideoBackupConfig;
import top.weixiansen574.bilibiliArchive.util.JSONConfig;


//@Table("video_backup_configs")
public class VideoBackupConfigInfo {
    private Integer id;
    public String desc;
   // @ValueForField("config")
    public String config;

    public int getId() {
        return id;
    }

  //  @ValueForSelect("id")
    public void setId(int id) {
        this.id = id;
    }

    public VideoBackupConfigInfo() {
    }

    public VideoBackupConfigInfo(String desc, VideoBackupConfig config) {
        this.desc = desc;
        setVideoBackupConfig(config);
    }

    public VideoBackupConfig getVideoBackupConfig(){
        return JSON.parseObject(config,VideoBackupConfig.class);
    }

    public void setVideoBackupConfig(VideoBackupConfig config){
        this.config = JSONConfig.toFormattedJSONString(config);
    }

    @Override
    public String toString() {
        return "VideoBackupConfigBean{" +
                "id=" + id +
                ", desc='" + desc + '\'' +
                ", configJsonText='" + config + '\'' +
                '}';
    }
}
