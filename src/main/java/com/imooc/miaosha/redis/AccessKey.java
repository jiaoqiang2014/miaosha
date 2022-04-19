package com.imooc.miaosha.redis;

public class AccessKey extends BasePrefix {

    public static AccessKey access = new AccessKey(5, "access");

    public AccessKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static AccessKey withExpire(int expireSeconds){
        return new AccessKey(expireSeconds, "access");
    }
}
