package top.weixiansen574.bilibiliArchive.exceptions;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;

public class IllegalConfigException extends Exception{
    Object config;

    public IllegalConfigException(Object config) {
        super(JSON.toJSONString(config, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteMapNullValue));
        this.config = config;
    }
}