package top.weixiansen574.bilibiliArchive.biliApis;

import com.alibaba.fastjson.annotation.JSONField;
import org.jetbrains.annotations.Nullable;


public abstract class BaseResponse {

    public int code;

    @Nullable
    public String message;

    public int ttl;

    @JSONField(deserialize = false, serialize = false)
    public boolean isSuccess() {
        return this.code == 0;
    }


}