package com.yuyue.admin.web.config.spring;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author mcdull
 * @date 2021-07-21
 */
public class StringToLocalTimeConverter implements Converter<String, LocalTime> {

    @Override
    public LocalTime convert(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        return LocalTime.parse(source, DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

}
