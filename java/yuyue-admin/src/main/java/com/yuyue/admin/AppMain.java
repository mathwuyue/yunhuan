package com.yuyue.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.ArrayList;
import java.util.List;


/**
 * 应用启动类
 *
 * @author bowen
 * @date 2022-03-16
 */
@SpringBootApplication
@CrossOrigin // 允许跨越访问
public class AppMain extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(com.yuyue.admin.AppMain.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(com.yuyue.admin.AppMain.class, args);
    }
}
