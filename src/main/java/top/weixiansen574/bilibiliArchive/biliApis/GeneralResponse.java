package top.weixiansen574.bilibiliArchive.biliApis;

public class GeneralResponse<T> extends BaseResponse {
    public T data;
    public T getDataNotNull() throws BiliBiliApiException {
        if (!isSuccess() || data == null){
            throw new BiliBiliApiException(code,message,null);
        }
        return data;
    }
    public T getDataNotNull(String errorTips) throws BiliBiliApiException {
        if (!isSuccess() || data == null){
            throw new BiliBiliApiException(code,message,errorTips);
        }
        return data;
    }
}