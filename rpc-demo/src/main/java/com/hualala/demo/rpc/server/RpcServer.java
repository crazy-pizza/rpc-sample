package com.hualala.demo.rpc.server;

import com.hualala.demo.rpc.common.*;
import com.hualala.demo.rpc.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author YuanChong
 * @create 2018-11-10 17:03
 * @desc rpc服务端逻辑
 */
@Log4j2
@Component
@Profile("service")
public class RpcServer implements InitializingBean, ApplicationContextAware {

    @Autowired
    private RpcConfig config;
    @Autowired
    private ServiceRegistry registry;

    //RPC服务注册到这个容器内
    private static final Map<Class<?>, Object> serviceContainer = new ConcurrentHashMap();


    /**
     * 开启netty监听
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet()  {
        //先检查配置是否可用
        if (validateConfig(config)) {
            log.error("config not init. please configuration rpc config in yaml");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //netty操作
                    EventLoopGroup loopGroup = new NioEventLoopGroup();
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(loopGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {

                            socketChannel.pipeline().addLast(new RpcDecoding(RpcRequest.class)) //IN 解码
                                    .addLast(new RpcEncoding(RpcResponse.class))//OUT 编码
                                    .addLast(new RpcHandler(serviceContainer));//IN 处理器
                        }
                    });
                    //开启NIO监听
                    String serverHost = config.getServerHost();
                    Integer serverPort = config.getServerPort();
                    ChannelFuture future = bootstrap.bind(serverHost, serverPort).sync();
                    log.info("rpc sever is run now on server: {}, port: {}", serverHost, serverPort);
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    /**
     * RPC服务注册
     *
     * @param ac
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        //注册到zk
        this.registry.registry(config.getServerHost() + ":" + config.getServerPort());
        //获取注册RpcService的服务
        Map<String, Object> beanMap = ac.getBeansWithAnnotation(RpcService.class);
        beanMap.values().stream().forEach(bean -> {
            Class<?> clazz = bean.getClass().getAnnotation(RpcService.class).value();
            //服务注册
            serviceContainer.put(clazz, bean);
            //接口注册
            this.registry.registryService(clazz);
        });
    }

    /**
     * 检查配置
     *
     * @param config
     * @return
     */
    private boolean validateConfig(RpcConfig config) {
        return StringUtils.isEmpty(config.getServerHost())
                || StringUtils.isEmpty(config.getZkAddr())
                || config.getServerHost() == null
                || config.getServerPort() == 0;

    }
}
