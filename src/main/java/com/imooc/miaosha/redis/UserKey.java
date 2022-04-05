package com.imooc.miaosha.redis;

public class UserKey extends BasePrefix{

    public static UserKey getById = new UserKey("id");

    public UserKey(String prefix) {
        super(prefix);
    }

    @Override
    public int expireSeconds() {
        return 0;
    }
}
