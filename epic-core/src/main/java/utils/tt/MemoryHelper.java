package utils.tt;

import java.lang.reflect.Method;

import utils.Logger;


public class MemoryHelper {
    private MemoryHelper() {
    }

    private static Class<?> memoryClass = null;

    static {

        if (memoryClass == null) {
            // 12 @CorePlatformApi(status = CorePlatformApi.Status.STABLE)     @SystemApi(client = MODULE_LIBRARIES)
            // 13 bate @SystemApi(client = MODULE_LIBRARIES)
            memoryClass = Refunsafe.findClass("libcore.io.Memory");
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
            Method method = Refunsafe.getMethod(memoryClass, "unsafeBulkGet", Object.class, int.class, int.class, byte[].class, int.class, int.class, boolean.class);
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
            Method method = Refunsafe.getMethod(memoryClass, "peekByte", long.class);
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
            Method method = Refunsafe.getMethod(memoryClass, "pokeByte", long.class, byte.class);
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

    /*****************************基础方法***********************/


    public static void e(Throwable e) {
        Logger.e(e);
    }

}
