package top.weixiansen574.bilibiliArchive;

import top.weixiansen574.bilibiliArchive.exceptions.ConfigFileException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) throws SQLException, ConfigFileException, IOException, ArchiveManger.ConfigListMismatchException {
        File path = new File(System.getProperty("user.dir"));
        if (args != null && args.length > 0 && args[0] != null){
            path = new File(args[0]);
        }
        System.out.println("当前路径："+path);
        BiliBiliArchive biliBiliArchive = new BiliBiliArchive(path);
        biliBiliArchive.startBackup();
    }
}
