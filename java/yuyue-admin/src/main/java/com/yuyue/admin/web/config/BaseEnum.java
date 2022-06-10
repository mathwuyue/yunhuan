package com.yuyue.admin.web.config;

import com.baomidou.mybatisplus.annotation.IEnum;

/**
 *
 * @author mcdull
 * @date 2021-07-19
 */
public interface BaseEnum<T> extends IEnum {

    String getName();

}
