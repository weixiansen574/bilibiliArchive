package top.weixiansen574.bilibiliArchive.archive;

import top.weixiansen574.bilibiliArchive.util.MiscUtils;

public class CMAvatar {
    public CMAvatar(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public CMAvatar() {
    }

    public String name;
    public byte[] data;


    @Override
    public String toString() {
        return "CMAvatar{" +
                "name='" + name + '\'' +
                ", data=" + data+
                '}';
    }
}
