package top.weixiansen574.bilibiliArchive;

import top.weixiansen574.bilibiliArchive.biliApis.BiliBiliApiException;
import top.weixiansen574.bilibiliArchive.exceptions.ConfigFileException;
import top.weixiansen574.bilibiliArchive.exceptions.usermanger.IllegalCookieException;
import top.weixiansen574.bilibiliArchive.exceptions.usermanger.UserHasExistsException;
import top.weixiansen574.bilibiliArchive.http.HttpLogger;
import top.weixiansen574.bilibiliArchive.util.FileUtil;
import top.weixiansen574.bilibiliArchive.util.JSONConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserManger {
    private final List<BiliUser> biliUsers;
    private final File usersDir;

    public UserManger(File usersDir, ArchiveManger archiveManger, HttpLogger logger) throws IOException, ConfigFileException {
        this.usersDir = usersDir;
        this.biliUsers = new ArrayList<>();
        File[] userDirList = usersDir.listFiles();
        if (userDirList != null) {
            for (File userDir : userDirList) {
                if (!userDir.isDirectory()){
                    continue;
                }
                BiliUser user = new BiliUser(archiveManger,logger,userDir);
                if (!userDir.getName().equals(String.valueOf(user.getUid()))){
                    throw new ConfigFileException(userDir.getAbsolutePath()+
                            "的文件夹名与配置文件中的UID不一致！请不要乱改配置文件");
                }
                System.out.println("已加载用户：" + user.getUid());
                biliUsers.add(user);
            }
        }
    }

    public BiliUser createAndAddNewUser(HttpLogger logger,String cookie) throws IllegalCookieException, BiliBiliApiException, IOException, UserHasExistsException {
        String uidText = getUidFromCookie(cookie);
        if (uidText == null){
            throw new IllegalCookieException("无法从cookie中获取UID！");
        }
        long uid = Long.parseLong(uidText);
        if (getUserByUid(uid) != null){
            throw new UserHasExistsException(uid);
        }
        File userDir = new File(usersDir, uidText);
        if (!userDir.mkdir()) {
            throw new RuntimeException("创建用户文件夹失败！");
        }

        BiliUser biliUser;
        try {
            biliUser = BiliUser.createNew(logger, userDir, cookie);
        } catch (IOException | BiliBiliApiException | IllegalCookieException e) {
            //若创建失败，删除残留的用户文件夹
            FileUtil.deleteDirs(userDir);
            throw e;
        }

        biliUsers.add(biliUser);
        return biliUser;
    }

    public void removeUser(long uid){
        for (int i = 0; i < biliUsers.size(); i++) {
            if (biliUsers.get(i).getUid() == uid){
                biliUsers.remove(i);
                break;
            }
        }
        File userDir = new File(usersDir,String.valueOf(uid));
        if (userDir.exists()) {
            FileUtil.deleteDirs(userDir);
        }
    }

    public List<BiliUser> getAllUser() {
        return biliUsers;
    }

    public BiliUser getUserByUid(long uid) {
        for (BiliUser biliUser : biliUsers) {
            if (biliUser.getUid() == uid) {
                return biliUser;
            }
        }
        return null;
    }

    public static String getUidFromCookie(String cookie){
        return getCookieValue(cookie, "DedeUserID");
    }

    public static String getCookieValue(String cookieString, String cookieName) {
        String[] cookies = cookieString.split("; ");
        for (String cookie : cookies) {
            String[] parts = cookie.split("=");
            if (parts.length == 2 && parts[0].equals(cookieName)) {
                return parts[1];
            }
        }
        return null;
    }
}
