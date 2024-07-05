package top.weixiansen574.bilibiliArchive.downloaders;

import okhttp3.OkHttpClient;
import top.weixiansen574.bilibiliArchive.ArchiveManger;
import top.weixiansen574.bilibiliArchive.archive.CMAvatar;
import top.weixiansen574.bilibiliArchive.biliApis.*;
import top.weixiansen574.bilibiliArchive.util.MiscUtils;
import top.weixiansen574.bilibiliArchive.util.OkHttpUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CommentDownloader extends ContentDownloader {

    public static final int TYPE_VIDEO = 1;


    public static final int SORT_BY_TIME = 0;
    public static final int SORT_BY_LIKE = 1;
    public static final int SORT_BY_REPLY = 2;

    public CommentDownloader(OkHttpClient httpClient, BiliApiService biliApiService, ArchiveManger archiveManger) {
        super(httpClient, biliApiService, archiveManger);
    }

    /**
     *
     * @param oid 评论区oid
     * @param type 评论区type
     * @param rootLimit 根评论限制，为-1则不限制
     * @param sortType 排序类型
     * @param timeLimit 时间锚点，到多久前的评论就不进行下载（需要按时间排序）,为0则不使用
     * @param replyUsePreview 下载评论回复是否只使用预览中的回复（用于减少回复评论的数量）
     * @throws IOException
     * @throws BiliBiliApiException
     */
    public void downloadOrUpdateComment(long oid, int type, int rootLimit, int sortType, long timeLimit,
                                        boolean replyUsePreview) throws IOException, BiliBiliApiException {
        if (timeLimit != 0 && sortType != SORT_BY_TIME) {
            throw new IllegalArgumentException("使用时间范围的方式下载（更新）评论区，排序类型必须为按时间！");
        }
        int pageNumber = 1;
        int savedRootCount = 0;
        int savedReplyCount = 0;

        //下载第一页评论
        GeneralResponse<CommentPage> response = OkHttpUtil
                .executeCall(biliApiService.getCommentPage(sortType, pageNumber, oid, type));
        if (response.isSuccess()) {
            CommentPage page = response.data;
            int totalCount = page.page.count;
            if (replyUsePreview && totalCount <= rootLimit) {
                System.out.println("由于评论区评论总数小于根评论数量限制，评论回复的下载模式改为所有！");
                replyUsePreview = false;
            }
            //保存第一页的顶置评论，顶置评论所有页都有且相同，所以只下载第一页的
            savedRootCount += saveTopComments(page);
            savedReplyCount += saveTopCommentReplies(page);
            flipPage:while (true) {
                List<BiliComment> comments = page.replies;
                //翻页到没有评论的时候停止翻页
                if (comments == null || comments.size() == 0) {
                    break;
                }
                for (BiliComment comment : comments) {
                    saveCommentAndAvatarAndPictures(comment);
                    savedReplyCount += downloadReplies(comment, replyUsePreview);
                    savedRootCount++;
                    System.out.printf("\r已下载评论[%d:%d/%d][%d][%s]", savedRootCount,
                            savedRootCount + savedReplyCount,totalCount, comment.rpid,
                            MiscUtils.omit(comment.getMessage(), 30).replace("\n", " "));
                    //下载的根评论数量满足要求后退出翻页
                    if (rootLimit != -1 && savedRootCount >= rootLimit){
                        System.out.printf("\n下载的根评论数量已达到[%d/%d]，完成下载！%n", savedRootCount, rootLimit);
                        break flipPage;
                    }
                    if (comment.ctime <= timeLimit) {
                        System.out.printf("\n下载的评论已到时间锚点位置[时间锚点：%d <= 当前评论：%d]，完成下载！%n",
                                timeLimit, comment.ctime);
                        break flipPage;
                    }
                }
                page = OkHttpUtil.getData(biliApiService.getCommentPage(sortType, ++pageNumber, oid, type));
            }
            System.out.println("\n评论下载完毕");
        } else if (response.code == 12061) {
            //评论区已关闭
            System.out.println("因为：" + response.message + "，跳过评论下载");
        } else {
            throw new BiliBiliApiException(response, "获取评论页失败");
        }
    }

    /**
     * 下载评论回复
     * @param rootComment 根评论
     * @param usePreview  true:从根评论的预览列表获取评论 false:api请求获取所有评论
     * @return 下载的评论数量
     */
    public int downloadReplies(BiliComment rootComment, boolean usePreview) throws IOException, BiliBiliApiException {
        int count = 0;
        //如果评论回复预览评论的数量与评论声明的回复数量一致，则直接在预览列表中取，节约时间
        if (rootComment.replies != null && rootComment.replies.size() == rootComment.rcount){
            usePreview = true;
        }
        if (usePreview) {
            if (rootComment.replies == null) {
                return count;
            }
            for (BiliComment reply : rootComment.replies) {
                saveCommentAndAvatarAndPictures(reply);
                count++;
            }
            return count;
        }

        for (int pn = 1; true; pn++) {
            CommentReplyPage replyPage = OkHttpUtil.getData(biliApiService.getCommentReplyPage(rootComment.rpid,
                    rootComment.oid, pn, rootComment.type));
            List<BiliComment> replies = replyPage.replies;
            if (replies == null || replies.size() == 0) {
                break;
            }
            for (BiliComment reply : replies) {
                System.out.printf("\r正在下载评论回复[%d/%d][%d][%s]", count, rootComment.rcount, reply.rpid,
                        MiscUtils.omit(reply.getMessage(), 30).replace("\n", " "));
                saveCommentAndAvatarAndPictures(reply);
                count++;
            }
        }
        return count;
    }

    private int saveTopComments(CommentPage cPage) throws IOException {
        List<BiliComment> topReplies = cPage.top_replies;
        int count = 0;
        if (topReplies != null) {
            for (BiliComment topReply : topReplies) {
                saveCommentAndAvatarAndPictures(topReply);
                count++;
            }
        }
        return count;
    }

    private int saveTopCommentReplies(CommentPage cPage) throws IOException, BiliBiliApiException {
        List<BiliComment> topReplies = cPage.top_replies;
        int count = 0;
        if (topReplies != null) {
            for (BiliComment topReply : topReplies) {
                //TODO 置顶评论无视使用预览？
                count += downloadReplies(topReply,false);
            }
        }
        return count;
    }

    private void saveCommentAndAvatarAndPictures(BiliComment comment) throws IOException {
        //下载头像
        String avatarUrl = comment.member.avatar;
        //头像文件名，也就是URL路径后面的那串文件名
        String avatarName = MiscUtils.getEndPathForHttpUrl(avatarUrl);
        //检查是否已下载过头像，若没有，则下载
        if (!archiveManger.checkCommentAvatarExists(avatarName)) {
            CMAvatar cmAvatar = new CMAvatar(MiscUtils.getEndPathForHttpUrl(avatarUrl), downloadContentAndRetry(
                    createDownloadRequest(avatarUrl+"@160w_160h_1c_1s_!web-avatar-comment.avif"), 15));
            archiveManger.insertCommentAvatar(cmAvatar);
        }
        //下载评论图片
        downloadCommentPicturesIfNotExists(comment);
        //保存评论到数据库
        archiveManger.insertCommentOrUpdateLike(comment);
    }

    /**
     * 下载评论图片,跳过已下载的
     *
     * @throws IOException 重试多遍还未下载成功
     */
    private void downloadCommentPicturesIfNotExists(BiliComment comment) throws IOException {
        String[] pictures = comment.getPictureUrls();
        if (pictures != null) {
            for (String pUrl : pictures) {
                File file = archiveManger.newCommentPictureFile(MiscUtils.getEndPathForHttpUrl(pUrl));
                if (!file.exists()) {
                    System.out.print("\r下载图片：" + pUrl);
                    downloadContentAndRetry(createDownloadRequest(pUrl), file, 10);
                } else {
                    System.out.print("\r图片：" + pUrl + "下载过，已跳过！");
                }
            }
        }
    }


}
