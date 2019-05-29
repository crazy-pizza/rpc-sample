package com.hualala.demo.rpc.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author YuanChong
 * @create 2018-11-10 17:06
 * @desc  用户所配置的rpc信息
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rpc-demo")
public class RpcConfig {

    private String serverHost;
    private Integer serverPort;
    private String zkAddr;


}
