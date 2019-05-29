package com.hualala.rpc;

import com.hualala.demo.rpc.server.RpcService;
import com.hualala.rpc.hello.HelloRpcInterface;
import com.hualala.rpc.hello.Persion;
import lombok.extern.log4j.Log4j2;

/**
 * @author YuanChong
 * @create 2018-11-12 10:40
 * @desc
 */
@Log4j2
@RpcService(HelloRpcInterface.class)
public class HelloRpcService implements HelloRpcInterface {

    @Override
    public Persion helloRpc(Persion persion) {

        log.info("服务端接收到请求 参数:{}", persion);

        persion.setAge(18);
        persion.setName("林青霞");

        return persion;
    }
}
