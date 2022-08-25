package utils.gs;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Constructor;
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


    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... types) {
        if (clazz == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        Method method = null;
        while (clazz != Object.class) {
            try {
                method = clazz.getDeclaredMethod(methodName, types);

                if (method != null) {
                    method.setAccessible(true);
                    return method;
                }
            } catch (Throwable e) {
            }
            clazz = clazz.getSuperclass();
        }
        return method;
    }

    public static Object newInstance(Constructor constructor, Object... args) {
        if (constructor == null) {
            return null;
        }
        try {
            return constructor.newInstance(args);
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Object invoke(Object obj, Method method, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            if (args == null || args.length < 1) {
                return method.invoke(obj);
            } else {
                return method.invoke(obj, args);
            }
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Field getField(String className, String fieldName) {
        return getField(findClass(className), fieldName);
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        Field field = null;
        while (clazz != null && clazz != Object.class) {
            try {
                field = clazz.getDeclaredField(fieldName);
                if (field != null) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (Exception e) {
            }
            clazz = clazz.getSuperclass();
        }
        return field;
    }

    public static Object getFieldValue(String className, String fieldName) {
        return getFieldValue(className, fieldName, null);
    }

    public static Object getFieldValue(String className, String fieldName, Object instance) {
        return getFieldValue(findClass(className), fieldName, instance);
    }

    public static Object getFieldValue(Class<?> clazz, String fieldName) {
        return getFieldValue(clazz, fieldName, null);
    }

    public static Object getFieldValue(Class<?> clazz, String fieldName, Object instance) {
        try {
            Field addr = getField(clazz, fieldName);
            if (addr != null) {
                return addr.get(instance);
            }
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Object call(String className, String methodName, Object receiver) {
        return call(findClass(className), methodName, receiver);
    }

    public static Object call(String className, String methodName, Object receiver,
                              Class[] types, Object[] params) {
        return call(findClass(className), methodName, receiver, types, params);
    }

    public static Object call(Class<?> clazz, String methodName, Object receiver) {
        return call(clazz, methodName, receiver, null, null);
    }

    public static Object call(Class<?> clazz, String methodName, Object receiver,
                              Class[] types, Object[] params) {
        try {

            if (types == null || params == null) {
                Method method = getMethod(clazz, methodName);
                return invoke(receiver, method);
            } else {
                Method method = getMethod(clazz, methodName, types);
                return invoke(receiver, method, params);
            }

        } catch (Throwable throwable) {
            e(throwable);
        }
        return null;
    }

//    //double
//    public static Object dCall(Class<?> clazz, String[] mns, Object receiver,
//                               Class[] types, Object[] params) {
//        try {
//            Method method = null;
//
//            if (mns != null || mns.length > 0) {
//                for (String mn : mns) {
//                    if (types == null || params == null) {
//                        method = getMethod(clazz, mn);
//                        if (method != null) {
//                            return invoke(receiver, method);
//                        }
//                    } else {
//                        method = getMethod(clazz, mn, types);
//                        if (method != null) {
//                            return invoke(receiver, method);
//                        }
//                    }
//                }
//            }
//        } catch (Throwable throwable) {
//            e(throwable);
//        }
//        return null;
//    }

    public static Object get(Class<?> clazz, String className, String fieldName, Object receiver) {
        try {
            if (clazz == null) clazz = findClass(className);
            return getFieldValue(clazz, fieldName, receiver);
        } catch (Throwable throwable) {
            e(throwable);
        }
        return null;
    }

    /**************************************工具方法***********************************/
    public static void e(Throwable e) {
        Log.e("sanbo.Refunsafe", Log.getStackTraceString(e));
    }
}
