package top.weixiansen574.bilibiliArchive.task;

import top.weixiansen574.bilibiliArchive.backupitems.BackupItem;

import java.util.List;

public class BackupTask implements Runnable {
    private List<BackupItem> backingUpItems;

    public BackupTask(List<BackupItem> backingUpItems) {
        this.backingUpItems = backingUpItems;
    }

    @Override
    public void run() {
        for (int i = 1; i <= backingUpItems.size(); i++) {
            BackupItem backupItem = backingUpItems.get(i-1);
            System.out.printf("正在备份[%d/%d][%s]%n",i,backingUpItems.size(),backupItem.getDesc());
            try {
                backupItem.execBackup();
            } catch (Throwable e) {
                e.printStackTrace();
                System.out.println("在备份项目："+backupItem.getDesc()+" 时发生"+e+"异常，本次备份循环失败！");
                return;
            }
        }
        System.out.println("已完成所有备份项目");
    }
}
