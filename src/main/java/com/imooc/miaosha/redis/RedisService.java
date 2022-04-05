package com.imooc.miaosha.redis;

import com.alibaba.fastjson.JSON;
import com.imooc.miaosha.controller.GoodsController;
import com.imooc.miaosha.domain.MiaoshaUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
public class RedisService {
    @Autowired
    JedisPool jedisPool;
    private static Logger log = LoggerFactory.getLogger(RedisService.class);

    public <T> T get(KeyPrefix prefix, String key, Class<T> clazz) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            log.info(realKey);
            String str = jedis.get(realKey);
            log.info(str);
            T t = stringToBean(str, clazz);
            return t;
        }finally {
            returnToPool(jedis);
        }
    }

    public <T> boolean set(KeyPrefix prefix, String key, T value) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String str = beanToString(value);
            String realKey = prefix.getPrefix() + key;
            int seconds = prefix.expireSeconds();   // 缓存有效时间
            if (seconds <= 0){
                jedis.set(realKey, str);
            }else {
                jedis.setex(realKey, seconds, str);
            }
            return true;
        }finally {
            returnToPool(jedis);
        }
    }

    private <T> String beanToString(T value) {
        if (value == null)
            return null;
        Class<?> clazz = value.getClass();
        if (clazz == Integer.class) {
            return "" + value;
        }else if (clazz == String.class){
            return (String) value;
        }else if (clazz == Long.class)
            return "" + clazz;
        else
            return JSON.toJSONString(value);
    }

    public <T> boolean exists(UserKey prefix, String key) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return jedis.exists(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    public <T> Long incr(UserKey prefix, String key) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return jedis.incr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }

    public <T> Long decr(UserKey prefix, String key) {
        Jedis jedis = null;
        try{
            jedis = jedisPool.getResource();
            String realKey = prefix.getPrefix() + key;
            return jedis.decr(realKey);
        }finally {
            returnToPool(jedis);
        }
    }


    private <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.length() <= 0 || clazz == null){
            return null;
        }else if (clazz == int.class || clazz == Integer.class){
            return (T) Integer.valueOf(str);
        }else if (clazz == String.class){
            return (T) str;
        }else if (clazz== long.class || clazz == Long.class)
            return (T) Long.valueOf(str);
        else
            return JSON.toJavaObject(JSON.parseObject(str), clazz);
    }

    private void returnToPool(Jedis jedis) {
        if (jedis != null){
            jedis.close();
        }
    }
}
