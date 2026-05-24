package de.fimatas.home.client.util;

import java.util.function.Function;

public class NullUtil {
    public static <T, R> R safeGet(T object, Function<T, R> getter) {
        return object == null ? null : getter.apply(object);
    }
}
