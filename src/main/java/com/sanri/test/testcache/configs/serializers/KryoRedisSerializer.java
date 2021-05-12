package com.sanri.test.testcache.configs.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * kryo 序列化工具
 */
public class KryoRedisSerializer implements RedisSerializer<Object> {
    private static final ThreadLocal<Kryo> kryoLocal = new ThreadLocal<Kryo>() {
        protected Kryo initialValue() {
            Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(
                    new StdInstantiatorStrategy()));
            return kryo;
        };
    };

    @Override
    public byte[] serialize(Object o) throws SerializationException {
        Kryo kryo = kryoLocal.get();
        Output output = new Output(1024, -1);
        kryo.writeClassAndObject(output,o);
        output.flush();
        return output.getBuffer();
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if(bytes == null)return null;
        Input input = new Input(bytes);
        Kryo kryo = kryoLocal.get();
        return kryo.readClassAndObject(input);
    }
}
