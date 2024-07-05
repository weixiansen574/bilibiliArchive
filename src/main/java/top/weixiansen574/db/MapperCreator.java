package top.weixiansen574.db;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.*;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.jetbrains.annotations.Nullable;

import javax.sql.DataSource;
import java.sql.SQLException;

public abstract class MapperCreator {

    protected final SqlSessionFactory factory;
    protected final SqlSession session;

    public MapperCreator(Class<?>[] mappers, DataSource dataSource,@Nullable DBVersionController versionController) throws SQLException {
        // 创建事务管理器
        JdbcTransactionFactory transactionFactory = new JdbcTransactionFactory();

        // 创建环境
        Environment environment = new Environment("development", transactionFactory, dataSource);

        // 创建配置
        Configuration configuration = new Configuration(environment);
        configuration.setCacheEnabled(false);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
        }
        factory = new SqlSessionFactoryBuilder().build(configuration);
        session = factory.openSession(true);
        DBExe dbExe = new DBExe(session.getConnection());
        init(dbExe);
        if (versionController != null){
            int dbVersion = getCurrentDBVersion(dbExe);
            int version = versionController.getVersion();
            if (version <= 0) {
                throw new IllegalArgumentException("VersionController Version cannot be less than or equal to 0");
            }
            if (dbVersion == 0) {
                versionController.onCreate(dbExe);
            } else if (dbVersion < version) {
                versionController.onUpgrade(dbExe, dbVersion, version);
            }
            setVersion(dbExe, version);
        }
    }

    public <T> T getMapper(Class<T> tClass){
        return session.getMapper(tClass);
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return factory;
    }

    protected abstract void init(DBExe dbExe) throws SQLException;

    protected abstract int getCurrentDBVersion(DBExe dbExe) throws SQLException;

    protected abstract void setVersion(DBExe dbExe,int newVersion) throws SQLException;

}
