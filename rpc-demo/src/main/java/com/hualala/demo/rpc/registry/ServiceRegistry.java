package com.hualala.demo.rpc.registry;

import com.hualala.demo.rpc.common.Constant;
import com.hualala.demo.rpc.common.RpcConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author YuanChong
 * @create 2018-11-11 17:43
 * @desc 本类为zookeeper服务注册中心 zookeeper为rpc提供了注册表的功能
 * 节点上注册了服务的地址和端口 并对客户端提供服务发现的功能
 */
@Log4j2
@Component
@Profile("service")
public class ServiceRegistry implements InitializingBean {

    @Autowired
    private RpcConfig config;

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private final Integer sessionTimeout = Integer.MAX_VALUE; //zk连接超时时间
    private ZooKeeper zooKeeper;
    private String serviceName;//记录每台服务器机器的注册信息


    /**
     * 服务机器注册 EPHEMERAL_SEQUENTIAL节点 支持多台服务端进行注册 客户端可以通过ZK实行负载均衡
     *
     * @param serviceAddr
     */
    public void registry(String serviceAddr) {
        try {
            if (zooKeeper.exists(Constant.zkNode, null) == null) {
                //创建目录
                zooKeeper.create(Constant.zkNode, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            //服务注册 持久有序节点
            /*
               zk的节点定义:
               sample-rpc:                                //主节点  持久化节点
                    data000000002:                        //每台服务一个节点 持久化顺序
                         server(ip : port)                //每台服务器的ip+端口  瞬时
                         zkInterface000001(Class)         //每台服务器的接口 瞬时顺序
                         zkInterface000001(Class)
             */
            serviceName = zooKeeper.create(Constant.zkData, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            //保存服务信息 临时节点
            zooKeeper.create(serviceName + "/server", serviceAddr.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            log.error("zookeeper register error msg {}", e.getMessage());
        }
    }

    /**
     * 具体的接口注册
     *
     * @param clazz
     */
    public void registryService(Class<?> clazz) {
        try {
            //接口注册 临时有序节点
            zooKeeper.create(serviceName + Constant.zkInterface, clazz.getName().getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }




    /**
     * 连接zk
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        ZooKeeper zooKeeper = new ZooKeeper(config.getZkAddr(), sessionTimeout, new Watcher() {
            @Override
            public void process(WatchedEvent watchedEvent) {
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    //这里防止zookeeper还没初始化连接成功就使用  使用了闭锁
                    countDownLatch.countDown();
                }
            }
        });
        countDownLatch.await();
        this.zooKeeper = zooKeeper;
    }
}
