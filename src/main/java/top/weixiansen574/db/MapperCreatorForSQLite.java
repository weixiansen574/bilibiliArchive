package top.weixiansen574.db;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MapperCreatorForSQLite extends MapperCreator{

    public MapperCreatorForSQLite(Class<?>[] mappers, File dbFile, @Nullable DBVersionController versionController) throws SQLException {
        super(mappers, getDataSource(dbFile), versionController);
    }

    @Override
    protected void init(DBExe dbExe) throws SQLException {
        dbExe.execSQL("PRAGMA FOREIGN_KEYS=ON;");
    }

    @Override
    protected int getCurrentDBVersion(DBExe dbExe) throws SQLException {
        Statement statement = dbExe.connection().createStatement();
        ResultSet resultSet = statement.executeQuery("PRAGMA user_version;");
        int userVersion = -1;
        if (resultSet.next()) {
            userVersion = resultSet.getInt(1);
        }
        resultSet.close();
        statement.close();
        return userVersion;
    }

    @Override
    protected void setVersion(DBExe dbExe, int newVersion) throws SQLException {
        Statement statement = dbExe.connection().createStatement();
        statement.execute("PRAGMA user_version = " + newVersion);
        statement.close();
    }

    public static DataSource getDataSource(File dbFile){
        UnpooledDataSource dataSource = new UnpooledDataSource();
        dataSource.setDriver("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:"+dbFile.getAbsolutePath());
        System.out.println(dataSource.getUrl());
        return dataSource;
    }
}
