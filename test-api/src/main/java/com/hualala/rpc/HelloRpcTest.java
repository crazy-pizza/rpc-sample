package com.hualala.rpc;

import com.hualala.Application;
import com.hualala.rpc.hello.HelloRpcInterface;
import com.hualala.rpc.hello.Persion;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author YuanChong
 * @create 2018-11-12 10:47
 * @desc
 */
@Log4j2
@RunWith(SpringRunner.class) //14.版本之前用的是SpringJUnit4ClassRunner.class
@SpringBootTest(classes = Application.class)
public class HelloRpcTest {

    @Autowired
    private HelloRpcInterface rpcInterface;

    @Test
    public void helloRpc() {
        Persion persion = new Persion();
        persion.setName("ZHANGSAN");
        persion.setAge(20);
        long l1 = System.currentTimeMillis();
        Persion result = rpcInterface.helloRpc(persion);
        long l2 = System.currentTimeMillis();
        log.info("接收到服务端返回数据: {}, 耗时: {}", result, (l2 - l1));

    }


}
