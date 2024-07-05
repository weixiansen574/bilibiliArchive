package top.weixiansen574.bilibiliArchive.dbmapper.versionctrl;

import top.weixiansen574.db.DBExe;
import top.weixiansen574.db.DBVersionController;

import java.sql.SQLException;

public class ArchiveInfosDBVersionControllerForSQLite implements DBVersionController {
    @Override
    public void onCreate(DBExe db) throws SQLException {
        db.execSQL("""
                    CREATE TABLE video_backup_configs (
                        id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        [desc] TEXT,
                        config TEXT
                    );
                    """);
        db.execSQL("""
                CREATE TABLE video_infos (
                    bvid                  TEXT    PRIMARY KEY
                                                  NOT NULL
                                                  UNIQUE,
                    avid                  INTEGER NOT NULL
                                                  UNIQUE,
                    title                 TEXT    NOT NULL,
                    [desc]                TEXT,
                    owner_mid             INTEGER NOT NULL,
                    owner_name            TEXT    NOT NULL,
                    owner_avatar_url      TEXT,
                    [view]                INTEGER NOT NULL,
                    danmaku               INTEGER NOT NULL,
                    favorite              INTEGER NOT NULL,
                    coin                  INTEGER NOT NULL,
                    [like]                INTEGER NOT NULL,
                    share                 INTEGER NOT NULL,
                    reply                 INTEGER NOT NULL,
                    tname                 TEXT,
                    ctime                 INTEGER NOT NULL,
                    cover_url             TEXT,
                    pages                 TEXT,
                    state                 INTEGER,
                    downloading           INTEGER NOT NULL,
                    save_time             INTEGER NOT NULL
                                                  DEFAULT 0,
                    community_update_time INTEGER NOT NULL
                                                  DEFAULT 0,
                    config_id             INTEGER NOT NULL
                                                  DEFAULT 0
                                                  REFERENCES video_backup_configs (id)
                );
                """);
        db.execSQL("""
                    CREATE TABLE user_favorite_videos (
                        favorites_id INTEGER NOT NULL,
                        bvid         TEXT    NOT NULL
                                             REFERENCES video_infos (bvid),
                        avid         INTEGER NOT NULL
                                             REFERENCES video_infos (avid),
                        fav_time     INTEGER NOT NULL,
                        PRIMARY KEY (
                            favorites_id,
                            bvid
                        )
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX fav_id_index ON user_favorite_videos (
                        favorites_id
                    );
                    """);
        db.execSQL("""
                    CREATE TABLE user_history_videos (
                        uid     INTEGER NOT NULL,
                        bvid    TEXT    NOT NULL
                                        REFERENCES video_infos (bvid),
                        avid    INTEGER NOT NULL
                                        REFERENCES video_infos (avid),
                        view_at INTEGER NOT NULL,
                        PRIMARY KEY (
                            uid,
                            bvid
                        )
                    );
                    """);
        db.execSQL("""
                    CREATE INDEX uhv_uid_index ON user_history_videos (
                        uid
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
