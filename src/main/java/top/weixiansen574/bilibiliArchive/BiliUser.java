package top.weixiansen574.bilibiliArchive;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.fastjson.FastJsonConverterFactory;
import top.weixiansen574.bilibiliArchive.biliApis.BiliApiService;
import top.weixiansen574.bilibiliArchive.biliApis.BiliBiliApiException;
import top.weixiansen574.bilibiliArchive.biliApis.CalendarEvent;
import top.weixiansen574.bilibiliArchive.biliApis.Pfs;
import top.weixiansen574.bilibiliArchive.config.UserBackupConfig;
import top.weixiansen574.bilibiliArchive.config.UserConfig;
import top.weixiansen574.bilibiliArchive.downloaders.VideoDownloader;
import top.weixiansen574.bilibiliArchive.exceptions.usermanger.IllegalCookieException;
import top.weixiansen574.bilibiliArchive.http.CookieInterceptor;
import top.weixiansen574.bilibiliArchive.http.HttpLogger;
import top.weixiansen574.bilibiliArchive.http.LoggingAndRetryInterceptor;
import top.weixiansen574.bilibiliArchive.util.FileUtil;
import top.weixiansen574.bilibiliArchive.util.JSONConfig;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import java.io.File;
import java.io.IOException;

public class BiliUser {
    private ArchiveManger archiveManger;

    private OkHttpClient okHttpClient;
    private BiliApiService biliApiService;

    private File userConfigFile;
    private File backupConfigFile;
    private File avatarFile;

    private UserConfig userConfig;
    private UserBackupConfig backupConfig;

    private File userDir;

    private VideoDownloader videoDownloader;

    private BiliUser() {
    }

    protected static BiliUser createNew(HttpLogger logger,File userDir,String cookie) throws IOException, BiliBiliApiException, IllegalCookieException {
        BiliUser user = new BiliUser();
        user.userDir = userDir;
        user.initConfigFile();
        user.initHttpClient(logger,cookie);
        UserConfig userConfig = user.requestNewUserConfig(cookie,user.biliApiService);
        user.userConfig = userConfig;
        JSONConfig.writeToFile(user.userConfigFile,userConfig);
        UserBackupConfig backupConfig = UserBackupConfig.createNew();
        JSONConfig.writeToFile(user.backupConfigFile,backupConfig);
        user.backupConfig = backupConfig;
        user.updateAvatar();
        return user;
    }

    private void initHttpClient(HttpLogger logger, String cookie){
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new CookieInterceptor(cookie))
                .addInterceptor(new LoggingAndRetryInterceptor(logger, 15))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.bilibili.com")
                .addConverterFactory(FastJsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        this.okHttpClient = okHttpClient;
        this.biliApiService = retrofit.create(BiliApiService.class);
    }

    private void initConfigFile(){
        userConfigFile = new File(userDir, "user.json");
        backupConfigFile = new File(userDir, "backup_config.json");
        avatarFile = new File(userDir, "avatar.jpg");
    }

    public BiliUser(ArchiveManger archiveManger,HttpLogger logger, File userDir) throws IOException {
        this.userDir = userDir;
        this.archiveManger = archiveManger;
        initConfigFile();
        if (!userConfigFile.exists()){
            throw new RuntimeException(userConfigFile.getAbsolutePath()+"被误删！读取用户配置失败");
        }
        if (!backupConfigFile.exists()){
            throw new RuntimeException(backupConfigFile.getAbsolutePath()+"被误删！读取用户配置失败");
        }
        readBackupConfig();
        readUserConfig();

        initHttpClient(logger, getCookie());
        initDownloader();
    }


    public void updateUserConfig(String cookie,HttpLogger logger) throws IOException, BiliBiliApiException, IllegalCookieException {
        //新建一个httpClient，区别于自身的客户端，用于获取新配置
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(new CookieInterceptor(cookie))
                .addInterceptor(new LoggingAndRetryInterceptor(logger, 15))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.bilibili.com")
                .addConverterFactory(FastJsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        BiliApiService biliApiService = retrofit.create(BiliApiService.class);
        UserConfig config = requestNewUserConfig(cookie,biliApiService);
        if (config.uid != getUid()){
            throw new IllegalCookieException("从cookie获取到的UID与原UID不一致，原UID："+getUid()+" 获取到的UID："+config.uid);
        }
        JSONConfig.writeToFile(userConfigFile,userConfig);
        this.userConfig = config;
        updateAvatar();
        //若更新成功，http客户端与api服务一起更新
        this.okHttpClient = okHttpClient;
        this.biliApiService = biliApiService;
        initDownloader();
    }


    private UserConfig requestNewUserConfig(String cookie,BiliApiService biliApiService) throws IOException, BiliBiliApiException, IllegalCookieException {
        CalendarEvent calendarEvent  = OkHttpUtil.getData(biliApiService.getCalendarEvents());
        Pfs pfs = calendarEvent.pfs;
        if (pfs == null){
            throw new IllegalCookieException("未解析到属性：calendar.pfs，请检查cookie是否失效？");
        }
        CalendarEvent.Profile profile = pfs.profile;
        UserConfig config = new UserConfig();
        config.cookie = cookie;
        config.uname = profile.name;
        config.uid = profile.mid;
        config.pfs = pfs;
        return config;
    }

    private void initDownloader() {
        videoDownloader = new VideoDownloader(okHttpClient,biliApiService,archiveManger);
    }

    private void readBackupConfig() throws IOException {
        this.backupConfig = JSONConfig.readFromFile(backupConfigFile, UserBackupConfig.class);
    }

    private void readUserConfig() throws IOException {
        this.userConfig = JSONConfig.readFromFile(userConfigFile, UserConfig.class);
    }

    private void saveBackupConfig() throws IOException {
        JSONConfig.writeToFile(backupConfigFile,backupConfig);
    }

    public long getUid() {
        return userConfig.uid;
    }

    public String getUserName(){
        return userConfig.uname;
    }

    private void updateAvatar() throws IOException {
        Request request = new Request.Builder()
                .url(getAvatarUrl())
                .addHeader("Cookie", getCookie())
                .build();
        ResponseBody body = okHttpClient.newCall(request).execute().body();
        OkHttpUtil.responseBodyNotNull(body);
        FileUtil.outputToFile(body.bytes(),avatarFile);
    }

    public ArchiveManger getArchiveManger(){
        return archiveManger;
    }

    public String getAvatarUrl() {
        return userConfig.pfs.profile.face;
    }

    public File getAvatarFile() {
        return avatarFile;
    }

    public String getCookie() {
        return userConfig.cookie;
    }

    /**
     * 是否是大会员,根据大会员开通信息并计算失效时间
     */
    public boolean currentTimeIsVip(){
        if (userConfig.pfs.profile.vip == null){
            return false;
        }
        return userConfig.pfs.profile.vip.due_date > System.currentTimeMillis();
    }


    public VideoDownloader getVideoDownloader() {
        return videoDownloader;
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public BiliApiService getBiliApiService() {
        return biliApiService;
    }

    public UserBackupConfig getBackupConfig() {
        return backupConfig;
    }
}
