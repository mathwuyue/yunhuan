package com.yuyue.admin.web.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author :bowen
 * @date : 2022/3/10
 */
@Configuration
@MapperScan( {
        "com.yuyue.admin.dal.dao"
} )
public class MybatisPlusConfig {


    /**
     * mybatis-plus 性能分析拦截器<br>
     * 文档：http://mp.baomidou.com<br>
     */
//    @Bean
//    public PerformanceInterceptor performanceInterceptor () {
//        return new PerformanceInterceptor();
//    }


}
