package com.hualala.demo.rpc.common;


import lombok.Data;

/**
 * @author YuanChong
 * @create 2018-11-11 16:27
 * @desc rpc请求格式
 */
@Data
public class RpcRequest {
    private String tarceID;
    private Class<?> interfaceClass;
    private String methodName;
    private Class<?>[] paramClass;
    private Object[] paramValues;
}
