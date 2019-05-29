package com.hualala.demo.rpc.server;

import com.hualala.demo.rpc.common.RpcRequest;
import com.hualala.demo.rpc.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.log4j.Log4j2;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author YuanChong
 * @create 2018-11-11 16:33
 * @desc  rpc服务处理执行
 */
@Log4j2
public class RpcHandler extends SimpleChannelInboundHandler<RpcRequest> {

    //RPC服务注册到这个容器内
    private Map<Class<?>, Object> serviceContainer;

    public RpcHandler(Map<Class<?>, Object> serviceContainer) {
        this.serviceContainer = serviceContainer;
    }

    /**
     * rpc调用 封装结果数据
     * @param context
     * @param request
     * @throws Exception
     */
    @Override
    public void channelRead0(ChannelHandlerContext context, RpcRequest request) throws Exception {
        RpcResponse response  = new RpcResponse();
        response.setTraceID(request.getTarceID());
        try {
            Object result = handler(request);
            response.setResult(result);
        }catch (Throwable e) {
            response.setError(e);
        }
        //写入到out内进行序列化
        context.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);

    }


    /**
     * 调用业务方方法
     * @param request
     * @return
     * @throws Exception
     */
    public Object handler(RpcRequest request) throws Exception {
        //从rpc服务容器内拿到服务
        Object service = serviceContainer.get(request.getInterfaceClass());
        if (service == null) {
            log.error("{} rpc service has not register ", service.getClass().getSimpleName());
            throw new RuntimeException("rpc service has not register");
        }
        Class<?> interfaceClass = request.getInterfaceClass();
        Class<?>[] paramClass = request.getParamClass();
        Object[] paramValues = request.getParamValues();
        String methodName = request.getMethodName();

        Method method = interfaceClass.getMethod(methodName, paramClass);
        //拿到处理结果
        return method.invoke(service, paramValues);

    }
}
