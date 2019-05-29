package com.hualala.demo.rpc.common;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuff.runtime.RuntimeSchema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

/**
 * @author YuanChong
 * @create 2018-11-11 16:28
 * @desc 序列化工具类（基于 Protostuff 实现）
 */
public class SerializationUtil {

    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();

    private static Objenesis objenesis = new ObjenesisStd(true);

    /**
     * 获取类的schema
     *
     * @param cls
     * @return
     */
    private static <T> Schema<T> getSchema(Class<T> cls) {
        Schema<T> schema = (Schema<T>) cachedSchema.get(cls);
        if (schema == null) {
            schema = RuntimeSchema.getSchema(cls);
            cachedSchema.put(cls, schema);
        }
        return schema;

    }

    /**
     * 序列化（对象 -> 字节数组）
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {

        Schema<T> schema = getSchema((Class<T>) obj.getClass());
        LinkedBuffer linkedBuffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        byte[] bytes = ProtobufIOUtil.toByteArray(obj, schema, linkedBuffer);
        return bytes;
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {

        T obj = objenesis.newInstance(cls);
        Schema<T> schema = getSchema(cls);
        ProtobufIOUtil.mergeFrom(data, obj, schema);
        return obj;
    }
}