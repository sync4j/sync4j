package com.fathzer.sync4j.util;

import java.lang.reflect.Field;

public class PrivateFields {
    public static <T> T getFieldValue(Object object, String fieldName, Class<T> fieldType) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return fieldType.cast(field.get(object));
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field " + fieldName, e);
        }
    }
}
