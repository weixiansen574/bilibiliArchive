package top.weixiansen574.bilibiliArchive.dbmapper.versionctrl;

import top.weixiansen574.db.DBExe;
import top.weixiansen574.db.DBVersionController;

import java.sql.SQLException;

public class ContentUpdateDBVersionController implements DBVersionController {
    @Override
    public void onCreate(DBExe dbExe) throws SQLException {
        dbExe.execSQL("""
                CREATE TABLE video_update_plans (
                    id                        INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp                 INTEGER NOT NULL,
                    uid                       INTEGER NOT NULL,
                    bvid                      TEXT    NOT NULL,
                    avid                      INTEGER NOT NULL,
                    title                     TEXT,
                    update_comment            INTEGER NOT NULL,
                    update_video_and_danmaku  INTEGER NOT NULL,
                    comment_root_limit        INTEGER,
                    comment_sort_type         INTEGER,
                    comment_time_limit        INTEGER,
                    comment_reply_use_preview INTEGER,
                    do_not_update_comment_when_a_certain_quantity INTEGER,
                    video_quality             INTEGER,
                    video_codec_id            INTEGER
                    
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
