package top.weixiansen574.bilibiliArchive.exceptions.usermanger;

public class UserHasExistsException extends Exception {
    public final long uid;

    public UserHasExistsException(long uid) {
        super("已存在用户UID："+uid);
        this.uid = uid;
    }
}
