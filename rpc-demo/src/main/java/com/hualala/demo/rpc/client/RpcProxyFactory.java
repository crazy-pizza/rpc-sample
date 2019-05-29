package com.hualala.demo.rpc.client;

import lombok.Data;
import org.springframework.beans.factory.FactoryBean;

import java.util.List;

/**
 * @author YuanChong
 * @create 2018-11-11 20:40
 * @desc 客户端创建接口代理
 */
@Data
public class RpcProxyFactory<T> implements FactoryBean<T> {

    private Class<T> interfaceClass; //具体的要代理的接口
    private List<String> serviceAddrList;//服务的注册表
    /**
     * 工厂实例化对象
     *
     * @return
     * @throws Exception
     */
    @Override
    public T getObject() throws Exception {
        return new RpcProxy<T>(serviceAddrList).newInstance(interfaceClass);
    }

    /**
     * bean class
     *
     * @return
     */
    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    /**
     * 单例模式
     *
     * @return
     */
    @Override
    public boolean isSingleton() {
        return true;
    }
}
