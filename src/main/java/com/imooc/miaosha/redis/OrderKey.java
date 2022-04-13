package com.imooc.miaosha.redis;

import com.sun.org.apache.xpath.internal.operations.Or;

public class OrderKey extends BasePrefix{
    public OrderKey(String prefix) {
        super(prefix);
    }

    public static OrderKey getMiaoshaOrderByUidGid = new OrderKey("moug");
}
