package top.weixiansen574.bilibiliArchive.config.user;

public class CommentUpdateConfig {
    /**
     * 从头到尾下载评论区，下载过的评论将更新评论点赞数以及它的回复评论
     * 优点：从头到尾更新了一遍
     * 缺点：遇到评论上万的评论区更新起来就恶心了（可设置**已下载数**超过多少不更新）
     */
    public static final String MODE_ALL = "all";
    /**
     * 以时间为锚点，依据之前下载里最晚发布评论的发布时间做锚点，再按时间排序从新往旧下载，直到评论的时间戳低于此评论的时间戳时停止下载
     * 优点：节约时间，尽量下载更多的评论
     * 缺点：无法更新前面评论的点赞数
     */
    public static final String MODE_TIME_ANCHOR = "timeAnchor";
    /**
     * 就更新排与下载点赞排名靠前的评论，目的是为了更新他们的点赞数以及他的评论回复
     */
    public static final String MODE_TOP_LIKE = "topLike";//热门前几个，想更新热门评论点赞数但是不想多下评论建议此项
    public static final String MODE_TIME_ANCHOR_AND_TOP_LIKE = "timeAnchorAndTopLike";//TODO 时间锚点+点赞数。折中方案

    public String mode;
    public Long timeAnchor;
    public Integer rootLimit;
    public Boolean replyUsePreview;
    public Integer doNotUpdateWhenACertainQuantity;
}
