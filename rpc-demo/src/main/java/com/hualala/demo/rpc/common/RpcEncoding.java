package com.hualala.demo.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author YuanChong
 * @create 2018-11-11 16:22
 * @desc netty序列化
 */
public class RpcEncoding extends MessageToByteEncoder {

    private Class<?> clazz;

    public RpcEncoding(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 编码  对象-->字节
     * @param context
     * @param obj
     * @param byteBuf
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext context, Object obj, ByteBuf byteBuf) throws Exception {
        if (clazz.isInstance(obj)) {
            byte[] data = SerializationUtil.serialize(obj);
            byteBuf.writeInt(data.length);
            byteBuf.writeBytes(data);
        }
    }
}
