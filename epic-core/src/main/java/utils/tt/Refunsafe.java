package utils.tt;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class Refunsafe {

    public static Class findClass(String className) {
        try {
            return Class.forName(className);
        } catch (Throwable e) {
            e(e);
        }
        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (Throwable e) {
            e(e);
        }
        try {
            return new String().getClass().getClassLoader().loadClass(className);
        } catch (Throwable e) {
            e(e);
        }

        return null;
    }

    public static Method getMethod(String className, String methodName, Class<?>... types) {
        try {
            Class<?> clazz = findClass(className);
            if (clazz != null) {
                return getMethod(clazz, methodName, types);
            }
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... types) {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, types);
        } catch (Throwable e) {
//            e(e);
        }
        if (method == null) {
            try {
                method = clazz.getMethod(methodName, types);
            } catch (Throwable e) {
//                e(e);
            }
        }
        if (method != null) {
            method.setAccessible(true);
        }
        return method;
    }

    public static Object getField(String className, String fieldName, Object instance) {
        return getField(findClass(className), fieldName, instance);
    }

    public static Object getField(Class<?> clazz, String fieldName, Object instance) {
        try {
            Field addr = clazz.getDeclaredField(fieldName);
            if (addr != null) {
                addr.setAccessible(true);
                return addr.get(instance);
            }
        } catch (Throwable e) {
            try {
                Field addr = clazz.getField(fieldName);
                if (addr != null) {
                    addr.setAccessible(true);
                    return addr.get(instance);
                }
            } catch (Throwable ex) {
                e(ex);
            }
        }
        return null;
    }

    public static void e(Throwable e) {
        Log.e("sanbo.Refunsafe", Log.getStackTraceString(e));
    }
}
