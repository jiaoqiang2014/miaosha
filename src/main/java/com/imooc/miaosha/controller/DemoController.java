package com.imooc.miaosha.controller;

import com.imooc.miaosha.domain.User;
import com.imooc.miaosha.redis.RedisService;
import com.imooc.miaosha.redis.UserKey;
import com.imooc.miaosha.server.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.imooc.miaosha.result.CodeMsg;
import com.imooc.miaosha.result.Result;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @RequestMapping("/db/get")
    @ResponseBody
//    测试mysql连接是否成功
    public Result<User> dbGet(){
        User user = userService.getById(1);
        return Result.success(user);
    }

    @RequestMapping("/redis/get")
    @ResponseBody
//    测试redis连接是否成功
    public Result<User> redisGet(){
        User user = redisService.get(UserKey.getById,""+1, User.class);
        System.out.println(UserKey.getById);
        return Result.success(user);
    }

    @RequestMapping("/redis/set")
    @ResponseBody
//    测试redis连接是否成功
    public Result<Boolean> redisSet(){
        User user = new User();
        user.setId(1);
        user.setName("1111");
        Boolean bl = redisService.set(UserKey.getById,""+1, user);  // prefix作为前缀，方便别人set key1，覆盖掉这个key1。
        return Result.success(true);
    }


    @RequestMapping("/db/Tx")
    @ResponseBody
//    测试事务
    public Result<Boolean> dbTx(){
        userService.tx();
        return Result.success(true);
    }


    @RequestMapping("/")
    @ResponseBody
    String home() {
        return "Hello World!";
    }
    //1.rest api json输出 2.页面
    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> hello() {
        return Result.success("hello,imooc");
        // return new Result(0, "success", "hello,imooc");
    }

    @RequestMapping("/helloError")
    @ResponseBody
    public Result<String> helloError() {
        return Result.error(CodeMsg.SERVER_ERROR);
        //return new Result(500102, "XXX");
    }

    @RequestMapping("/thymeleaf")
    public String  thymeleaf(Model model) {
        model.addAttribute("name", "Joshua");
        return "hello";
    }

}
