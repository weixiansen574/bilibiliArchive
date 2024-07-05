package top.weixiansen574.bilibiliArchive.dbmapper.versionctrl;

import top.weixiansen574.db.DBExe;
import top.weixiansen574.db.DBVersionController;

import java.sql.SQLException;

public class CommentDBVersionController implements DBVersionController {
    @Override
    public void onCreate(DBExe db) throws SQLException {
        db.execSQL("""
                    CREATE TABLE comments (
                        rpid          INTEGER NOT NULL
                                              PRIMARY KEY,
                        oid           INTEGER NOT NULL,
                        type          INTEGER NOT NULL,
                        mid           INTEGER NOT NULL,
                        root          INTEGER NOT NULL,
                        parent        INTEGER NOT NULL,
                        uname         TEXT    NOT NULL,
                        current_level INTEGER NOT NULL,
                        location      TEXT,
                        message       TEXT    NOT NULL,
                        [like]        INTEGER NOT NULL,
                        ctime         INTEGER NOT NULL,
                        pictures      TEXT,
                        avatar_url    TEXT    NOT NULL
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX oid_index ON comments (
                        oid
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX type_index ON comments (
                        type
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX root_index ON comments (
                        root
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX avatar_url_index ON comments (
                        avatar_url
                    );
                    """);
        db.execSQL("CREATE INDEX idx_oid_root ON comments (oid, root);");
        db.execSQL("""
                    CREATE INDEX like_index ON comments (
                        "like" DESC
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX ctime_index ON comments (
                        ctime DESC
                    );
                    """);
        db.execSQL("""
                    CREATE TABLE avatars (
                        name TEXT PRIMARY KEY
                                  UNIQUE
                                  NOT NULL,
                        data BLOB
                    );
                    """);
    }

    @Override
    public void onUpgrade(DBExe dbExe, int oldVersion, int newVersion) throws SQLException {

    }

    @Override
    public int getVersion() {
        return 1;
    }
}
