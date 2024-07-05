package top.weixiansen574.bilibiliArchive.config;

public class CommentDownloadConfig {
    public static final String COMMENT_CFG_ALL = "all";
    public static final String COMMENT_CFG_NOT_ARCHIVE = "notArchive";
    public static final String COMMENT_CFG_TOP_LIKE = "topLike";
    /**
     * 调用评论回复的api下载楼中楼所有回复评论
     */
    public static final String REPLY_CFG_ALL = "all";
    /**
     * 保存在replies[x].replies的评论，不再调用/reply/reply?root=xxx的api去获取评论回复
     * 设置这个可有效避免下载的评论回复大幅超出预期的情况
     * 由于不再调用评论回复api获取评论，所以速度更快
     */
    public static final String REPLY_CFG_IN_PREVIEW = "inPreview";
    /**
     * 由于评论回复无法使用热门排序，所以只能下载干净（下载干净有助于生成完美的mermaid评论回复树状图！）
     * 当然下载评论回复在如下载历史记录这种你只希望保存点赞排名靠前100的评论，但是把回复一起加上就下了甚至1000条才停止。可以设置只下载预览列表里的评论回复以避免这种情况
     */
   // public String replyCommentCfg;
    public String commentCfg;
    public boolean replyUsePreview;
    public int commentTopLike;
    //public String commentReplyCfg;
    //public int commentReplyTopLike;
    //public int updateCommentFrequency;//数据库里不保存已下载次数，关闭备份姬重新打开不会恢复进度
    //public long updateCommentInterval;
    //public int updateCommentLimit;//更新评论数量限制，大于此数时不再更新评论


    public CommentDownloadConfig(String commentCfg,boolean replyUsePreview, int commentTopLike) {
        this.commentCfg = commentCfg;
        this.commentTopLike = commentTopLike;
        this.replyUsePreview = replyUsePreview;
    }

    public boolean check(){
        return commentCfg != null && (commentCfg.equals(COMMENT_CFG_ALL) || commentCfg.equals(COMMENT_CFG_NOT_ARCHIVE) ||
                commentCfg.equals(COMMENT_CFG_TOP_LIKE));
    }
}
