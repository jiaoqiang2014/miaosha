package com.imooc.miaosha.config;

import com.imooc.miaosha.access.UserContext;
import com.imooc.miaosha.domain.MiaoshaUser;
import com.imooc.miaosha.redis.MiaoshaUserKey;
import com.imooc.miaosha.server.MiaoshaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class UserArgumentResolvers implements HandlerMethodArgumentResolver {

    @Autowired
    MiaoshaUserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 当为 MiaoshaUser 类型时就执行参数解析，即调用下面的 resolveArgument 方法。
        Class<?> clazz = parameter.getParameterType();
        return clazz== MiaoshaUser.class;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {
//        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
//        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
//
//        String paramToken = request.getParameter(MiaoshaUserService.COOKI_NAME_TOKEN);
//        String cookieToken = getCookieValue(request, MiaoshaUserService.COOKI_NAME_TOKEN);
//
//        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)){
//            return null;
//        }
//        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
//        return userService.getByToken(response,token);

        return UserContext.getUser();
    }

//    private String getCookieValue(HttpServletRequest request, String cookieName) {
//        Cookie[] cookies = request.getCookies();
//        if (cookies == null || cookies.length <= 0){
//            return null;
//        }
//        for (Cookie cookie : cookies) {
//            if (cookie.getName().equals(cookieName)){
//                return cookie.getValue();
//            }
//        }
//        return null;
//    }
}
