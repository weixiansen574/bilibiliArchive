package top.weixiansen574.db;

import org.apache.ibatis.session.SqlSession;

import java.sql.SQLException;

public interface DBVersionController {

    void onCreate(DBExe dbExe) throws SQLException;
    void onUpgrade(DBExe dbExe, int oldVersion, int newVersion) throws SQLException;
    int getVersion();

}
