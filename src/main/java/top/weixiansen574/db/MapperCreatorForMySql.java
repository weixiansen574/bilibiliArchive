package top.weixiansen574.db;

import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.SQLException;

//TODO implement
public class MapperCreatorForMySql extends MapperCreator{

    public MapperCreatorForMySql(Class<?>[] mappers, DataSource dataSource, @Nullable DBVersionController versionController) throws SQLException {
        super(mappers, dataSource, versionController);
    }

    @Override
    protected void init(DBExe dbExe) throws SQLException {

    }

    @Override
    protected int getCurrentDBVersion(DBExe dbExe) throws SQLException {
        return 0;
    }

    @Override
    protected void setVersion(DBExe dbExe, int newVersion) throws SQLException {

    }
}
