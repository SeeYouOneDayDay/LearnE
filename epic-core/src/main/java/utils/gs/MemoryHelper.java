package utils.gs;

import java.lang.reflect.Method;

import utils.Logger;


public class MemoryHelper {
    private MemoryHelper() {
    }

    private static Class<?> memoryClass = null;

    static {

        if (memoryClass == null) {
            memoryClass = Refunsafe.findClass("libcore.io.Memory");
        }
    }


    public static void memcpy(long dst, long src, long length) {
        for (long i = 0; i < length; i++) {
            pokeByte(dst, peekByte(src));
            dst++;
            src++;
        }
    }

    public static byte peekByte(long address) {
        // public static native byte peekByte(long address);
        Method method = Refunsafe.getMethod(memoryClass, "peekByte", long.class);
        return ((Byte) Refunsafe.invoke(null, method, address)).byteValue();
    }

    public static void pokeByte(long address, byte value) {
        // public static native void pokeByte(long address, byte value);
        Method method = Refunsafe.getMethod(memoryClass, "pokeByte", long.class, byte.class);
        Refunsafe.invoke(null, method, address, value);
    }





    /*****************************基础方法***********************/


    private static void d(String s) {
        Logger.d(s);
    }

    private static void i(String s) {
        Logger.i(s);
    }

    public static void e(Throwable e) {
        Logger.e(e);
    }
}
