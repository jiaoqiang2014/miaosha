package com.imooc.miaosha.rabbitmq;
import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.server.GoodsService;
import com.imooc.miaosha.server.MiaoshaService;
import com.imooc.miaosha.server.MiaoshaUserService;
import com.imooc.miaosha.server.OrderServer;
import com.imooc.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MQReceiver {

    private static Logger log = LoggerFactory.getLogger(MQReceiver.class);

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderServer orderServer;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    RedisService redisService;

    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE) // 指定从 MQConfig.MIAOSHA_QUEUE 这个queue里边读数据
    public void receive(String message){
        log.info("receive message:" + message);
        MiaoshaMessage mm = RedisService.stringToBean(message, MiaoshaMessage.class);
        MiaoshaUser miaoshaUser= mm.getUser();
        long goodsId = mm.getGoodsId();

        // 判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        log.info("商品ID：" + goods.getId() + " 库存为: " + goods.getStockCount() + " 名称：" + goods.getGoodsName());
        int stock = goods.getStockCount();
        if (stock <= 0){
            return ;
        }
        // 判断是否重复下单,生成用户对应的秒杀订单表示已经秒杀到了。
        MiaoshaOrder order = orderServer.getMiaoshaOrderByUserIdGoodsId(miaoshaUser.getId(), goodsId);
        if (order != null){
            log.info("重复秒杀");
            return ;
        }

        // 减库存，下订单，写入秒杀订单（原子操作）
        OrderInfo orderInfo = miaoshaService.miaosha(miaoshaUser, goods);
        log.info("订单名：" + orderInfo.getGoodsName());
    }

    /*
    @RabbitListener(queues = MQConfig.QUEUE) // 指定从MQConfig.QUEUE这个queue里边读数据
    public void receive(String message){
        log.info("receive message:" + message);
    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE1)
    public void receiveTopic1(String message){
        log.info("topic queue1 message:" + message);
    }

    @RabbitListener(queues = MQConfig.TOPIC_QUEUE2)
    public void receiveTopic2(String message){
        log.info("topic queue2 message:" + message);
    }

    @RabbitListener(queues = MQConfig.HEADERS_QUEUE)
    public void receiveHeader(byte[] message){
        log.info("header queue message:" + new String(message));
    }
    */
}
