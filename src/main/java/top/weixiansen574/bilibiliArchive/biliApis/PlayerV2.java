package top.weixiansen574.bilibiliArchive.biliApis;

import java.util.List;

public class PlayerV2 {
    public long aid;
    public String bvid;
    public boolean allow_bp;
    public boolean no_share;
    public long cid;
    public int max_limit;
    public int page_no;
    public boolean has_next;
    public IpInfo ip_info;
    public long login_mid;
    public String login_mid_hash;
    public boolean is_owner;
    public String name;
    public String permission;
    public LevelInfo level_info;
    public Vip vip;
    public int answer_status;
    public int block_time;
    public String role;
    public int last_play_time;
    public long last_play_cid;
    public long now_time;
    public int online_count;
    public boolean need_login_subtitle;
    public Subtitles subtitle;
    public String preview_toast;
    public Options options;
    public OnlineSwitch online_switch;
    public Fawkes fawkes;
    public ShowSwitch show_switch;
    public boolean toast_block;
    public boolean is_upower_exclusive;
    public boolean is_upower_play;
    public boolean is_ugc_pay_preview;
    public ElecHighLevel elec_high_level;
    public boolean disable_show_up_info;

    public static class IpInfo {
        public String ip;
        public String zone_ip;
        public int zone_id;
        public String country;
        public String province;
        public String city;
    }

    public static class Vip {
        public int type;
        public int status;
        public long due_date;
        public int vip_pay_type;
        public int theme_type;
        public Label label;
        public int avatar_subscript;
        public String nickname_color;
        public int role;
        public String avatar_subscript_url;
        public int tv_vip_status;
        public int tv_vip_pay_type;
        public long tv_due_date;
        public AvatarIcon avatar_icon;

        public static class Label {
            public String path;
            public String text;
            public String label_theme;
            public String text_color;
            public int bg_style;
            public String bg_color;
            public String border_color;
            public boolean use_img_label;
            public String img_label_uri_hans;
            public String img_label_uri_hant;
            public String img_label_uri_hans_static;
            public String img_label_uri_hant_static;
        }

        public static class AvatarIcon {
            public int icon_type;
            public IconResource icon_resource;

            public static class IconResource {
                // This is empty in the provided JSON, define fields here if needed
            }
        }
    }


    public static class Subtitles {
        public boolean allow_submit;
        public String lan;
        public String lan_doc;
        public List<Subtitle> subtitles;
    }

    public static class Options {
        public boolean is_360;
        public boolean without_vip;
    }

    public static class OnlineSwitch {
        public String enable_gray_dash_playback;
        public String new_broadcast;
        public String realtime_dm;
        public String subtitle_submit_switch;
    }

    public static class Fawkes {
        public int config_version;
        public int ff_version;
    }

    public static class ShowSwitch {
        public boolean long_progress;
    }

    public static class ElecHighLevel {
        public int privilege_type;
        public String title;
        public String sub_title;
        public boolean show_button;
        public String button_text;
        public String jump_url;
        public String intro;
    }
}
