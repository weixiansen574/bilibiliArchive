package top.weixiansen574.bilibiliArchive.config;

import top.weixiansen574.bilibiliArchive.biliApis.Pfs;

public class UserConfig {
    public long uid;
    public String uname;
    public String cookie;
    public Pfs pfs;
    //private File configFile;

    public UserConfig(){};

    public UserConfig(long uid, String uname, String cookie, Pfs pfs) {
        this.uid = uid;
        this.uname = uname;
        this.cookie = cookie;
        this.pfs = pfs;
    }
}
