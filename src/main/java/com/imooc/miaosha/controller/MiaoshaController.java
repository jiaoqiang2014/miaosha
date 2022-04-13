package com.imooc.miaosha.controller;

import com.imooc.miaosha.domain.MiaoshaOrder;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.domain.OrderInfo;
import com.imooc.miaosha.rabbitmq.MQSender;
import com.imooc.miaosha.rabbitmq.MiaoshaMessage;
import com.imooc.miaosha.redis.GoodsKey;
import com.imooc.miaosha.redis.MiaoshaKey;
import com.imooc.miaosha.redis.OrderKey;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;
import com.imooc.miaosha.server.GoodsService;
import com.imooc.miaosha.server.MiaoshaService;
import com.imooc.miaosha.server.MiaoshaUserService;
import com.imooc.miaosha.server.OrderServer;
import com.imooc.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.BinaryClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    private static Logger log = LoggerFactory.getLogger(MiaoshaController.class);

    private Map<Long, Boolean> localOverMap = new HashMap();
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
    @Autowired
    MQSender sender;

    /*
     * 实现 InitializingBean 接口，就会主动调用这个接口。
     * */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 加载秒杀的信息
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if (goodsList == null){
            return ;
        }

        // 将秒杀信息写入redis
        for (GoodsVo goods : goodsList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
            localOverMap.put(goods.getId(), false); // false 表示秒杀库存还有，秒杀没有结束。
        }
    }

  /*
    *//*
    *
    * 没有使用页面静态化
    * QPS: 1474
    * 4000 * 10
    *
    * 出现了买超现象，下面代码单个用户是没有问题的，但多个用户时，可能多个用户同时判断库存和检查重复秒杀通过，都进入购买环节，导致失败。
    * *//*
    @RequestMapping("/do_miaosha")
    public String list(Model model, MiaoshaUser miaosUser, @RequestParam("goodsId")long goodsId){
        // @RequestParam("goodsId")long goodsId 从 good_detail.html 中的
            //        <td>
            //        	<form id="miaoshaForm" method="post" action="/miaosha/do_miaosha">
            //        		<button class="btn btn-primary btn-block" type="submit" id="buyButton">立即秒杀</button>
            //        		<input type="hidden" name="goodsId" th:value="${goods.id}" />
            //        	</form>
            //        </td>
        // 的接受goodsId值，long类型。
        model.addAttribute("user", miaosUser);
        if(miaosUser == null){
            return "login";
        }

        // 判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        int stock = goods.getGoodsStock();
        if (stock <= 0){
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
            log.info("库存不足");
            return "miaosha_fail";
        }
        // 判断是否重复下单,生成用户对应的秒杀订单表示已经秒杀到了。
        MiaoshaOrder order = orderServer.getMiaoshaOrderByUserIdGoodsId(miaosUser.getId(), goodsId);
        if (order != null){
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAOSHA.getMsg());
            log.info("重复秒杀");
            return "miaosha_fail";
        }

        // 减库存，下订单，写入秒杀订单（原子操作）
        OrderInfo orderInfo = miaoshaService.miaosha(miaosUser, goods);
        model.addAttribute("orderInfo", orderInfo); // 秒杀成功后将订单信息直接写入到页面上
        model.addAttribute("goods", goods);
        model.addAttribute("miaosUser", miaosUser);
        log.info("秒杀之后订单信息：" + orderInfo.getGoodsName(), orderInfo.getGoodsCount(),orderInfo.getGoodsId());
        log.info("秒杀之后商品信息：" + goods.getGoodsDetail(), goods.getGoodsName() + " 秒杀之后的库存：" + goods.getGoodsStock());
        return "order_detail";
    }
  */

    /*
     * QPS: 1474
     * 4000 * 10
     *
     * 出现了买超现象，下面代码单个用户是没有问题的，但多个用户时，可能多个用户同时判断库存和检查重复秒杀通过，都进入购买环节，导致失败。
     * */
    @RequestMapping(value = "/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser miaosUser, @RequestParam("goodsId")long goodsId){
        model.addAttribute("user", miaosUser);
        if(miaosUser == null){
            return Result.error(CodeMsg.SERVER_ERROR);
        }

        /*
        // 判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        log.info("商品ID：" + goods.getId() + " 库存为: " + goods.getStockCount() + " 名称：" + goods.getGoodsName());
        int stock = goods.getStockCount();
        if (stock <= 0){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        // 判断是否重复下单,生成用户对应的秒杀订单表示已经秒杀到了。
        MiaoshaOrder order = orderServer.getMiaoshaOrderByUserIdGoodsId(miaosUser.getId(), goodsId);
        if (order != null){
            log.info("重复秒杀");
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        // 减库存，下订单，写入秒杀订单（原子操作）
        OrderInfo orderInfo = miaoshaService.miaosha(miaosUser, goods);
        log.info("订单名：" + orderInfo.getGoodsName());
        return Result.success(orderInfo);
         */

        // 调用内存标记，查看是否秒杀结束，可以减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over){
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        // 预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);    //
        if (stock < 0){
            localOverMap.put(goodsId, true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        // 判断是否重复下单,生成用户对应的秒杀订单表示已经秒杀到了。
        MiaoshaOrder order = orderServer.getMiaoshaOrderByUserIdGoodsId(miaosUser.getId(), goodsId);
        if (order != null){
            log.info("重复秒杀");
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }

        // 入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(miaosUser);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0);   // 0 表示排队中
    }

    /*
    @RequestMapping("/to_detail/{goodsId}")
    public String detail(Model model, MiaoshaUser user, @PathVariable("goodsId")long goodsId){
        model.addAttribute("user", user);
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods", goods);

        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if (now < startAt){     // 秒杀没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        }else if (now > endAt){
            miaoshaStatus = 2;
            remainSeconds = -1;
        }else{
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        return "goods_detail";
    }
*/

    /*
    * 返回：
    * orderId:成功
    * -1：秒杀失败
    * 0：排队中
    * */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser miaosUser, @RequestParam("goodsId")long goodsId) {
        model.addAttribute("user", miaosUser);
        if (miaosUser == null) {
            return Result.error(CodeMsg.SERVER_ERROR);
        }
        long result = miaoshaService.getMiaoshaResult(miaosUser.getId(), goodsId);
        return Result.success(result);
    }

    @RequestMapping(value="/reset", method=RequestMethod.GET)
    @ResponseBody
    public Result<Boolean> reset(Model model) {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        for(GoodsVo goods : goodsList) {
            goods.setStockCount(10);
            redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), 10);
            localOverMap.put(goods.getId(), false);
        }
        redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
        redisService.delete(MiaoshaKey.isGoodsOver);
        miaoshaService.reset(goodsList);
        return Result.success(true);
    }
}
