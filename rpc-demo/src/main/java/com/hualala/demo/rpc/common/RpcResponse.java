package com.hualala.demo.rpc.common;

import lombok.Data;

/**
 * @author YuanChong
 * @create 2018-11-11 16:28
 * @desc rpc响应格式
 */
@Data
public class RpcResponse {

    private Throwable error;
    private Object result;
    private String traceID;


    public Boolean isError() {
        return error != null;
    }

}
