package com.hualala.demo.rpc.client;


import com.hualala.demo.rpc.common.RpcRequest;
import com.hualala.demo.rpc.common.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author YuanChong
 * @create 2018-11-11 21:58
 * @desc  接口代理
 */
public class RpcProxy<T> {


    private List<String> serviceAddrList;//服务的注册表

    public RpcProxy(List<String> serviceAddrList) {
        this.serviceAddrList = serviceAddrList;
    }

    /**
     * 解析获取服务端地址
     *
     * @return
     */
    public String discover() {
        //这里的负载均衡的策略简单的采用了随机数的方式
        int index = ThreadLocalRandom.current().nextInt(serviceAddrList.size());
        return serviceAddrList.get(index);
    }


    T newInstance(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                //封装请求参数
                RpcRequest request = new RpcRequest();
                request.setTarceID(UUID.randomUUID().toString());
                request.setInterfaceClass(clazz);
                request.setMethodName(method.getName());
                request.setParamClass(method.getParameterTypes());
                request.setParamValues(args);
                //获取服务地址
                String serverAddr = discover();
                //netty
                RpcClient client = new RpcClient(serverAddr.split(":")[0], Integer.parseInt(serverAddr.split(":")[1]));
                //netty传输数据到服务端
                RpcResponse response = client.send(request);
                if(response.isError()) {
                    throw response.getError();
                }
                return response.getResult();
            }
        });
    }
}
