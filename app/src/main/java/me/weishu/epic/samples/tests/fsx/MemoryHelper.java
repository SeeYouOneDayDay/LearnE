package me.weishu.epic.samples.tests.fsx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import utils.Logger;
import utils.UnsafeHelper;

public class MemoryHelper {
    private MemoryHelper() {
    }

    private static Class<?> memoryClass = null;

    static {

        if (memoryClass == null) {
            // 12 @CorePlatformApi(status = CorePlatformApi.Status.STABLE)     @SystemApi(client = MODULE_LIBRARIES)
            // 13 bate @SystemApi(client = MODULE_LIBRARIES)
            memoryClass = findClass("libcore.io.Memory");
        }
    }

    /**
     * Used to optimize nio heap buffer bulk get operations. 'dst' must be a primitive array.
     * 'dstOffset' is measured in units of 'sizeofElements' bytes.
     *
     */
    public static void unsafeBulkGet(Object dst, int dstOffset, int byteCount, byte[] src, int srcOffset, int sizeofElements, boolean swap) {
        try {
//    public static native void unsafeBulkGet(Object dst, int dstOffset, int byteCount,  byte[] src, int srcOffset, int sizeofElements, boolean swap);
            Method method = getMethod(memoryClass, "unsafeBulkGet", Object.class, int.class, int.class, byte[].class, int.class, int.class, boolean.class);
            if (method != null) {
                method.invoke(null, dst, dstOffset, byteCount, srcOffset, srcOffset, sizeofElements, swap);
            }
        } catch (Throwable e) {
            e(e);
        }
    }

    // libcode.io.Memory#peekByte
    private static byte peekByte(long address) {
        //   @UnsupportedAppUsage
        //    @FastNative
        //    public static native byte peekByte(long address);
        try {
            Method method = getMethod(memoryClass, "peekByte", long.class);
            if (method != null) {
                return (Byte) method.invoke(null, address);
            }
        } catch (Throwable e) {
            e(e);
        }
        return (byte) 0;
    }

    static void pokeByte(long address, byte value) {
        // @UnsupportedAppUsage
        //    @FastNative
        //    public static native void pokeByte(long address, byte value);
        try {
            Method method = getMethod(memoryClass, "pokeByte", long.class, byte.class);
            if (method != null) {
                method.invoke(null, address, value);
            }
        } catch (Throwable e) {
            e(e);
        }
    }

    public static void memcpy(long dst, long src, long length) {
        for (long i = 0; i < length; i++) {
            pokeByte(dst, peekByte(src));
            dst++;
            src++;
        }
    }

    public static long getMethodAddress(Method method) {
        try {
            Object mirrorMethod = getField(Method.class.getSuperclass(), "artMethod", method);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
            return getObjectAddress(mirrorMethod);
        } catch (Throwable e) {

        }
        return 0;
    }


    public static long getObjectAddress(Object o) {
        Object[] objects = {o};
        Integer baseOffset = UnsafeHelper.arrayBaseOffset(Object[].class);
        if (UnsafeHelper.is64Bit()) {
            return UnsafeHelper.getLong(objects, baseOffset.longValue());
        } else {
            return UnsafeHelper.getInt(objects, baseOffset.longValue());
        }
    }



    /*****************************基础方法***********************/
    private static Object getField(Class<?> clazz, String fieldName, Object instance) {
        try {
            Field addr = clazz.getDeclaredField(fieldName);
            if (addr != null) {
                addr.setAccessible(true);
            }
            return addr.get(instance);
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

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

    public static void e(Throwable e) {
        Logger.e(e);
    }

}
