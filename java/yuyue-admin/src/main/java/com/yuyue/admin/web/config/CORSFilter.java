package com.yuyue.admin.web.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CORSFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        List<String> allowedOrigins = Arrays.asList("http://41.190.21.46:8100","http://172.16.0.100:8100","http://localhost:8100","http://localhost:9528", "http://localhost:9529", "http://192.168.2.131:9529", "http://192.168.2.131:9528", "http://192.168.2.11:8082", "http://41.199.2.51:8082");

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        String origin = request.getHeader("Origin");
        //允许所有的域访问
        response.setHeader("Access-Control-Allow-Origin", allowedOrigins.contains(origin) ? origin : "*");
        //允许所有方式的请求
        response.setHeader("Access-Control-Allow-Methods", "*");
        //头信息缓存有效时长（如果不设 Chromium 同时规定了一个默认值 5 秒），没有缓存将已OPTIONS进行预请求
        response.setHeader("Access-Control-Max-Age", "3600");
        //允许的头信息
//        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, authorization");
        //特别注意，前端提示跨域请求，解决前端Provisional headers are shown问题
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, Authorization, token, content-type,userId,dataPermission,partyDataPermission");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }
}
