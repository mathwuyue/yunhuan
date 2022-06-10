package com.yuyue.admin.web.config.spring;

import com.yuyue.admin.web.config.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 *
 * @author mcdull
 * @date 2021-07-19
 */
public class IntegerToEnumConverterFactory implements ConverterFactory<Integer, BaseEnum> {

    @Override
    public <E extends BaseEnum> Converter<Integer, E> getConverter(Class<E> targetType) {
        return new IntegerToEnum(targetType);
    }

    private class IntegerToEnum<E extends BaseEnum> implements Converter<Integer, E> {
        private final Class<E> enumType;

        IntegerToEnum(Class<E> enumType) {
            this.enumType = enumType;
        }

        @Override
        public E convert(Integer source) {
            if (source == null) {
                return null;
            }
            return getEnum(enumType, source);
        }

        private E getEnum(Class<E> targetType, Integer source) {
            for (E enumObj : targetType.getEnumConstants()) {
                if (source.equals(enumObj.getValue())) {
                    return enumObj;
                }
            }
            return null;
        }
    }
}
