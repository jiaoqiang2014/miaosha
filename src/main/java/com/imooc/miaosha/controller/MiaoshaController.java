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
import com.imooc.miaosha.util.MD5Util;
import com.imooc.miaosha.util.UUIDUtil;
import com.imooc.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.BinaryClient;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
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


    @RequestMapping(value="/verifyCode", method=RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCod(HttpServletResponse response, MiaoshaUser user,
                                              @RequestParam("goodsId")long goodsId) {
        if(user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try {
            BufferedImage image  = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }

    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(Model model, MiaoshaUser miaosUser,
                                         @RequestParam("goodsId")long goodsId,
                                         @RequestParam("verifyCode")int verifyCode) {
        model.addAttribute("user", miaosUser);
        if (miaosUser == null) {
            return Result.error(CodeMsg.SERVER_ERROR);
        }

        boolean check = miaoshaService.checkVerifyCode(miaosUser, goodsId, verifyCode);
        if(!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        String path = miaoshaService.creteMiaoshaPath(miaosUser, goodsId);
        return Result.success(path);
    }

    /*
     * QPS: 1474
     * 4000 * 10
     *
     * 出现了买超现象，下面代码单个用户是没有问题的，但多个用户时，可能多个用户同时判断库存和检查重复秒杀通过，都进入购买环节，导致失败。
     * */
    @RequestMapping(value = "{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser miaosUser, @RequestParam("goodsId")long goodsId, @PathVariable("path") String path){
        model.addAttribute("user", miaosUser);
        if(miaosUser == null){
            return Result.error(CodeMsg.SERVER_ERROR);
        }

        // 校验path
        boolean check = miaoshaService.checkPath(miaosUser, goodsId, path);
        if (!check){
            Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

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
