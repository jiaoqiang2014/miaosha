package com.imooc.miaosha.redis;

public class GoodsKey extends BasePrefix {

    public static GoodsKey getGoodsList = new GoodsKey(60, "gl");
    public static KeyPrefix getGoodsDetail = new GoodsKey(60, "gd");
    public static KeyPrefix getMiaoshaGoodsStock = new GoodsKey(0, "gs");


    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

}
