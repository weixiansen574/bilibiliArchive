package top.weixiansen574.bilibiliArchive.archive;

import top.weixiansen574.bilibiliArchive.biliApis.Subtitle;

public class ArchiveSubtitle {
    public long id;
    public String lan;//language
    public String lan_doc;
    public String content;//json
    public ArchiveSubtitle(long id, String lan, String lan_doc, String content) {
        this.id = id;
        this.lan = lan;
        this.lan_doc = lan_doc;
        this.content = content;
    }

    public ArchiveSubtitle(Subtitle subtitle,String content) {
        this.id = subtitle.id;
        this.lan = subtitle.lan;
        this.lan_doc = subtitle.lan_doc;
        this.content = content;
    }

    public ArchiveSubtitle() {
    }


}
