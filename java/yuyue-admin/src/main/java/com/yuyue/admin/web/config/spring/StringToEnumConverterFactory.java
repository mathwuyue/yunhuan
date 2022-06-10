package com.yuyue.admin.web.config.spring;

import com.yuyue.admin.web.config.BaseEnum;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 *
 * @author mcdull
 * @date 2021-07-19
 */
public class StringToEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @Override
    public <E extends BaseEnum> Converter<String, E> getConverter(Class<E> targetType) {
        return new StringToEnum(targetType);
    }

    private class StringToEnum<E extends BaseEnum> implements Converter<String, E> {
        private final Class<E> enumType;

        StringToEnum(Class<E> enumType) {
            this.enumType = enumType;
        }

        @Override
        public E convert(String source) {
            if (StringUtils.isBlank(source)) {
                return null;
            }
            return getEnum(enumType, source);
        }

        private E getEnum(Class<E> targetType, String source) {
            for (E enumObj : targetType.getEnumConstants()) {
                if (source.equals(enumObj.getValue())) {
                    return enumObj;
                }
            }
            return null;
        }
    }
}

