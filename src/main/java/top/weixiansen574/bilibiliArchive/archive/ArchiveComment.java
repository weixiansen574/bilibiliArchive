package top.weixiansen574.bilibiliArchive.archive;


import top.weixiansen574.bilibiliArchive.biliApis.BiliComment;

public class ArchiveComment {
    public ArchiveComment(){}

    public long rpid;
    public long oid;
    public int type;
    public long mid;
    public long root;
    public long parent;
    public String uname;
    public int current_level;
    public String location;
    public String message;
    public int like;
    public long ctime;
    public String pictures;
    public String avatar_url;

    public ArchiveComment(BiliComment source){
        this.rpid = source.rpid;
        this.oid = source.oid;
        this.type = source.type;
        this.root = source.root;
        this.parent = source.parent;
        this.ctime = source.ctime;
        this.like = source.like;
        this.message = source.content.message;
        this.mid = source.member.mid;
        this.uname = source.member.uname;
        this.current_level = source.member.level_info.current_level;
        this.location = source.reply_control.location;
        this.avatar_url = source.member.avatar;
    }

    public String[] getAllPictureUrl(){
        if (pictures != null){
            return pictures.split(";");
        }
        return null;
    }
}

