package com.imooc.miaosha.redis;

public class OrderKey extends BasePrefix{
    public OrderKey(String prefix) {
        super(prefix);
    }

    @Override
    public int expireSeconds() {
        return 0;
    }
}
