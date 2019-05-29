package com.hualala.demo.rpc.client;

import com.hualala.demo.rpc.common.RpcDecoding;
import com.hualala.demo.rpc.common.RpcEncoding;
import com.hualala.demo.rpc.common.RpcRequest;
import com.hualala.demo.rpc.common.RpcResponse;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * @author YuanChong
 * @create 2018-11-11 22:09
 * @desc netty客户端
 */
@Data
public class RpcClient extends SimpleChannelInboundHandler<RpcResponse> {

    private String host;
    private int port;

    private RpcResponse rpcResponse;//服务器端返回结果

    private CountDownLatch countDown = new CountDownLatch(1);

    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * netty客户端数据传输
     *
     * @param request
     * @return
     */
    public RpcResponse send(RpcRequest request) throws Exception {
        EventLoopGroup nioEventExecutors = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(nioEventExecutors).channel(NioSocketChannel.class)
                    //绑定服务端的IP和端口
                    .remoteAddress(new InetSocketAddress(host, port)).handler(new ChannelInitializer() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast(new RpcEncoding(RpcRequest.class))//OUT 1 序列化
                            .addLast(new RpcDecoding(RpcResponse.class)) //IN 1  反序列化
                            .addLast(RpcClient.this); //IN 2
                }
            });
            //连接服务器
            ChannelFuture channelFuture = bootstrap.connect().sync();
            //将request对象写入outbundle处理后发出（即RpcEncoder编码器）
            channelFuture.channel().writeAndFlush(request).sync();
            //阻塞等待服务器端返回
            countDown.await();
            channelFuture.channel().closeFuture().sync();
            return rpcResponse;
        } finally {
            nioEventExecutors.shutdownGracefully().sync();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcResponse rpcResponse) throws Exception {
        this.rpcResponse = rpcResponse;
        countDown.countDown();
    }
}
