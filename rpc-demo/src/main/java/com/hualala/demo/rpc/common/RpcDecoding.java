package com.hualala.demo.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author YuanChong
 * @create 2018-11-11 16:22
 * @desc netty反序列化
 */
public class RpcDecoding extends ByteToMessageDecoder {

    private Class<?> clazz;

    public RpcDecoding(Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * 解码 从netty中拿到字节数据 转换对象
     * @param context
     * @param byteBuf
     * @param list
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf byteBuf, List<Object> list) throws Exception {
        if (!byteBuf.isReadable()) {
            context.close();
            return;
        }
        byteBuf.readableBytes();
        byteBuf.markReaderIndex();
        int dataLength = byteBuf.readInt();
        if (dataLength < 0) {
            context.close();
        }
        if (byteBuf.readableBytes() < dataLength) {
            byteBuf.resetReaderIndex();
        }
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data);
        //字节转换对象
        Object request = SerializationUtil.deserialize(data, this.clazz);
        list.add(request);
    }
}
