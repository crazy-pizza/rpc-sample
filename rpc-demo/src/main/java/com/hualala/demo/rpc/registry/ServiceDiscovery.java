package com.hualala.demo.rpc.registry;

import com.hualala.demo.rpc.client.RpcProxyFactory;
import com.hualala.demo.rpc.common.Constant;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author YuanChong
 * @create 2018-11-11 20:43
 * @desc
 */
@Log4j2
@Component
@Profile("api")
public class ServiceDiscovery implements InitializingBean, ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

    @Value("${rpc-demo.zkAddr}")
    private String zkAddr;

    private CountDownLatch countDownLatch = new CountDownLatch(1);
    private ZooKeeper zooKeeper;
    private final Integer sessionTimeout = Integer.MAX_VALUE; //zk连接超时时间

    private ApplicationContext applicationContext;

    private List<String> serviceAddrList;//服务的注册表
    private List<Class<?>> interfaceClass = new ArrayList<>(); //服务的注册接口信息


    /**
     * 获取服务端绑定的接口信息
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        //删除无用注册信息
        delZkRegistry();
        //获取第1台服务器的接口注册信息
        List<String> children = zooKeeper.getChildren(Constant.zkNode, false, null);
        String serviceName = children.get(0);
        //服务的节点名称
        String serviceNode = Constant.zkNode + "/" + serviceName;
        //获取服务与接口信息
        List<String> nodeInfo = zooKeeper.getChildren(serviceNode, false, null);
        for (String nodeName : nodeInfo) {
            if (nodeName.startsWith("zkInterface")) {
                //获取接口数据
                byte[] bytes = zooKeeper.getData(serviceNode + "/" + nodeName, false, null);
                //接口
                Class<?> clazz = Class.forName(new String(bytes));
                interfaceClass.add(clazz);
            }
        }
    }


    /**
     * 实现服务端可以水平扩展 这里增加一个ZK监听机制 监听是否有新的服务端加入
     */
    private void watchNode() {
        try {
            //删除无用注册信息
            delZkRegistry();
            List<String> children = zooKeeper.getChildren(Constant.zkNode, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getType() == Event.EventType.NodeChildrenChanged) {
                        //持续监听
                        watchNode();
                    }
                }
            });
            List<String> serviceAddrList = new ArrayList<>();
            //获取每一台机器的地址
            for (String nodeName : children) {
                byte[] bytes = zooKeeper.getData(Constant.zkNode + "/" + nodeName + "/server", false, null);
                serviceAddrList.add(new String(bytes));
            }
            log.info("sevice node change for {} ", serviceAddrList);
            //重新记录服务端地址
            this.serviceAddrList = serviceAddrList;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 初始化zk服务
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        try {
            this.applicationContext = applicationContext;

            if (StringUtils.isEmpty(zkAddr)) {
                zkAddr = readZkAddr();
            }
            zooKeeper = new ZooKeeper(zkAddr, sessionTimeout, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                        //这里防止zookeeper还没初始化连接成功就使用  使用了闭锁
                        countDownLatch.countDown();
                    }
                }
            });
            //监听zk节点变化
            watchNode();
            countDownLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 将接口代理对象纳入spring的管理中
     *
     * @param beanDefinitionRegistry
     * @throws BeansException
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        for (Class<?> clazz : interfaceClass) {
            //需要被代理的接口
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
            GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
            //为工厂设置interfaceClass的属性
            definition.getPropertyValues().add("interfaceClass", definition.getBeanClassName());
            definition.getPropertyValues().add("serviceAddrList", this.serviceAddrList);
            //代理工厂
            definition.setBeanClass(RpcProxyFactory.class);
            definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            // 注册bean名,一般为类名首字母小写
            beanDefinitionRegistry.registerBeanDefinition(clazz.getSimpleName(), definition);
        }

    }


    /**
     * 清除无用注册信息
     *
     * @throws KeeperException
     * @throws InterruptedException
     */

    public void delZkRegistry() {
        try {
            List<String> children = zooKeeper.getChildren(Constant.zkNode, false, null);
            for (String data : children) {
                List<String> dataChild = zooKeeper.getChildren(Constant.zkNode + "/" + data, false, null);
                //删除session过期节点
                if (dataChild == null || dataChild.size() == 0) {
                    zooKeeper.delete(Constant.zkNode + "/" + data, -1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }


    /**
     * 由于使用了BeanDefinitionRegistryPostProcessor 不能提前注入属性
     * 只能以流的形式读取配置
     *
     * @return
     */
    public String readZkAddr() throws Exception {

        File file = ResourceUtils.getFile("classpath:application.yml");
        FileInputStream fileInputStream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader br = new BufferedReader(inputStreamReader, 60 * 1024 * 1024);
        String zkAddr = null;
        while ((zkAddr = br.readLine()) != null) {
            if (zkAddr.contains("zkAddr")) {
                zkAddr = zkAddr.replaceAll("zkAddr:", "").trim();
                System.out.println(zkAddr);
                break;
            }
        }
        return zkAddr;
    }


}
