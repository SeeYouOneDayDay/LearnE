package utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class AA {
    public static long getConstructorAddress(Constructor constructor) {
        try {
            if (constructor == null) {
                return 0L;
            }
            Object mirrorMethod = Refunsafe.getFieldValue(Constructor.class.getSuperclass(), "artMethod", constructor);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
//            return UnsafeHelper.getObjectAddress(mirrorMethod);
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {

        }
        return 0L;
    }

    public static long getMethodAddress(Method method) {
        try {
            if (method == null) {
                return 0L;
            }
            Object mirrorMethod = Refunsafe.getFieldValue(Method.class.getSuperclass(), "artMethod", method);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {

        }
        return 0L;
    }

}
