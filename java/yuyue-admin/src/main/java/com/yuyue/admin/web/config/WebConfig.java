package com.yuyue.admin.web.config;

import com.yuyue.admin.web.config.spring.*;
import com.yuyue.admin.web.servlet.OkServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web层配置
 *
 * @author bowen
 * @date 2022-03-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("", HandlerTypePredicate.forBasePackage("com.yuyue.admin.web" +
                ".controller"));
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new StringToEnumConverterFactory());
        registry.addConverterFactory(new IntegerToEnumConverterFactory());
        registry.addConverter(new StringToLocalDateTimeConverter());
        registry.addConverter(new StringToLocalDateConverter());
        registry.addConverter(new StringToLocalTimeConverter());
    }

    @Bean
    public ServletRegistrationBean registerOkServlet() {
        ServletRegistrationBean servletBean = new ServletRegistrationBean(new OkServlet(), "/ok");
        return servletBean;
    }

}
