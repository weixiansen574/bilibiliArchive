package top.weixiansen574.bilibiliArchive;

import top.weixiansen574.bilibiliArchive.backupitems.BackupItem;
import top.weixiansen574.bilibiliArchive.backupitems.FavoritesBackup;
import top.weixiansen574.bilibiliArchive.backupitems.HistoryBackup;
import top.weixiansen574.bilibiliArchive.config.GlobalConfig;
import top.weixiansen574.bilibiliArchive.config.UserBackupConfig;
import top.weixiansen574.bilibiliArchive.config.user.FavoritesBackupConfig;
import top.weixiansen574.bilibiliArchive.config.user.HistoryBackupConfig;
import top.weixiansen574.bilibiliArchive.dbmapper.ContentUpdatePlanMapper;
import top.weixiansen574.bilibiliArchive.dbmapper.versionctrl.ContentUpdateDBVersionController;
import top.weixiansen574.bilibiliArchive.exceptions.ConfigFileException;
import top.weixiansen574.bilibiliArchive.http.HttpLogger;
import top.weixiansen574.bilibiliArchive.task.BackupTask;
import top.weixiansen574.bilibiliArchive.task.BackupTaskPublisher;
import top.weixiansen574.bilibiliArchive.task.ContentUpdatePublisher;
import top.weixiansen574.bilibiliArchive.task.TaskManger;
import top.weixiansen574.bilibiliArchive.util.FileUtil;
import top.weixiansen574.bilibiliArchive.util.JSONConfig;
import top.weixiansen574.db.MapperCreatorForSQLite;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BiliBiliArchive {
    public final File rootPath;
    public final File globalConfigFile;
    public HttpLogger httpLogger;
    public ArchiveManger archiveManger;
    public UserManger userManger;

    public TaskManger taskManger;
    public BackupTaskPublisher backupTaskPublisher;
    public ContentUpdatePublisher contentUpdatePublisher;

    private final GlobalConfig globalConfig;


    public BiliBiliArchive(File rootPath) throws SQLException, IOException, ConfigFileException, ArchiveManger.ConfigListMismatchException {
        this.rootPath = rootPath;
        this.globalConfigFile = new File(rootPath,"config.json");
        if (!globalConfigFile.exists()){
            globalConfig = GlobalConfig.createNew();
            updateGlobalConfig();
        } else {
            globalConfig = JSONConfig.readFromFile(globalConfigFile,GlobalConfig.class);
        }
        this.httpLogger = new HttpLogger(FileUtil.getOrCreateDir(rootPath,"logs"));
        this.archiveManger = new ArchiveManger(FileUtil.getOrCreateDir(rootPath,"archives"),
                FileUtil.getOrCreateDir(rootPath,"cache"),globalConfig.videoConfigIds);
        this.userManger = new UserManger(FileUtil.getOrCreateDir(rootPath,"users"),archiveManger,httpLogger);
        this.taskManger = new TaskManger();
        this.backupTaskPublisher = new BackupTaskPublisher(taskManger);
        ContentUpdatePlanMapper contentUpdatePlanMapper = new MapperCreatorForSQLite
                (new Class[]{ContentUpdatePlanMapper.class}, new File(rootPath,"ContentUpdateTasks.db"),
                        new ContentUpdateDBVersionController()).getMapper(ContentUpdatePlanMapper.class);
        this.contentUpdatePublisher = new ContentUpdatePublisher(taskManger,userManger, contentUpdatePlanMapper);
    }

    public synchronized StartupResult startBackup() {
        if (isRunning()){
            System.out.println("备份姬已在运行中");
            return new StartupResult(StartupResult.Status.hasStarted,null);
        }
        List<BiliUser> biliUsers = userManger.getAllUser();
        List<BackupItem> backingUpItems = new ArrayList<>();
        for (BiliUser biliUser : biliUsers) {
            List<BackupItem> allBackingUpItems = createBackingUpItems(biliUser);
            System.out.println("用户名：" + biliUser.getUserName() + " UID:" + biliUser.getUid() +
                    " 已加载"+allBackingUpItems.size()+"份备份项目");
            backingUpItems.addAll(allBackingUpItems);
        }
        if (backingUpItems.size() > 0) {
            //优先级排序
            Collections.sort(backingUpItems);
            BackupTask backupTask = new BackupTask(backingUpItems);
            taskManger.start();
            contentUpdatePublisher.start();
            backupTaskPublisher.start(backupTask,globalConfig.intervalOfLoop);
            return new StartupResult(StartupResult.Status.startSuccessful,null);
        } else {
            try {
                stop();
            } catch (InterruptedException ignored) {
            }
            System.out.println("没有任何备份配置启用，备份姬未运行");
            return new StartupResult(StartupResult.Status.failed,null);
        }
    }

    private List<BackupItem> createBackingUpItems(BiliUser user){
        UserBackupConfig backupConfig = user.getBackupConfig();
        HistoryBackupConfig historyBackupConfig = backupConfig.historyBackupConfig;
        List<FavoritesBackupConfig> favoritesBackupConfigs = backupConfig.favoritesBackupConfigs;
        List<BackupItem> items = new ArrayList<>();
        if (historyBackupConfig != null && historyBackupConfig.enable){
            items.add(new HistoryBackup(user,backupConfig.historyBackupConfig,null,contentUpdatePublisher));
        }
        if (favoritesBackupConfigs!= null) {
            for (FavoritesBackupConfig favoritesBackupConfig : backupConfig.favoritesBackupConfigs) {
                if (favoritesBackupConfig.enable) {
                    items.add(new FavoritesBackup(user, favoritesBackupConfig,contentUpdatePublisher));
                }
            }
        }
        return items;
    }

    public void updateGlobalConfig() throws IOException {
        JSONConfig.writeToFile(globalConfigFile,globalConfig);
    }

    public void checkNotRunning() throws CannotBeModifiedAtRunTimeException {
        if (isRunning()){
            throw new CannotBeModifiedAtRunTimeException();
        }
    }

    public boolean isRunning() {
        return taskManger.isRun();
    }

    public synchronized boolean stop() throws InterruptedException {
        backupTaskPublisher.stop();
        //communityUpdatePublisher.stop();
        return taskManger.stop();
    }

    /**
     * 不能在运行时修改异常，如运行时不能修改用户信息
     */
    public static class CannotBeModifiedAtRunTimeException extends Exception {
        public CannotBeModifiedAtRunTimeException() {
        }
        public CannotBeModifiedAtRunTimeException(String message) {
            super(message);
        }
    }

    //todo 测试的，非正式
 /*   public void start(){
        List<BiliUser> biliUsers = userManger.getAllUser();
        List<BackupItem> backingUpItems = new ArrayList<>();
        for (BiliUser biliUser : biliUsers) {
            List<BackupItem> allBackingUpItems = createBackingUpItems(biliUser,communityUpdatePublisher);
            System.out.println("用户名：" + biliUser.getUserName() + " UID:" + biliUser.getUid() +
                    " 已加载"+allBackingUpItems.size()+"份备份项目");
            backingUpItems.addAll(allBackingUpItems);
        }

        if (backingUpItems.size() > 0) {
            //优先级排序
            Collections.sort(backingUpItems);
            BackupTask backupTask = new BackupTask(backingUpItems);
            backupTask.setGarbageCollector(new GarbageCollector(userManger, archiveContext));
            taskManger.start();
            communityUpdatePublisher.start();
            backupTaskPublisher.start(backupTask,archiveContext.getIntervalOfLoop());
            return new StartupResult(StartupResult.Status.startSuccessful,null);
        } else {
            try {
                stop();
            } catch (InterruptedException ignored) {
            }
            System.out.println("没有任何备份配置启用，备份姬未运行");
            return new StartupResult(StartupResult.Status.failed,null);
        }
        System.out.println(biliBiliArchive.userManger.getAllUser());

    }

    private List<BackupItem> createBackingUpItems(BiliUser user,CommunityUpdatePublisher updatePublisher){
        UserBackupConfig backupConfig = user.getBackupConfig();
        HistoryBackupConfig historyBackupConfig = backupConfig.historyBackupConfig;
        List<FavoritesBackupConfig> favoritesBackupConfigs = backupConfig.favoritesBackupConfigs;
        List<BackupItem> items = new ArrayList<>();
        if (historyBackupConfig != null && historyBackupConfig.enable){
            items.add(new HistoryVideoBackup(user,backupConfig.historyBackupConfig,updatePublisher));
        }
        if (favoritesBackupConfigs!= null) {
            for (FavoritesBackupConfig favoritesBackupConfig : backupConfig.favoritesBackupConfigs) {
                if (favoritesBackupConfig.enable) {
                    items.add(new FavoritesBackup(user, favoritesBackupConfig, updatePublisher));
                }
            }
        }
        return items;
    }*/




}
