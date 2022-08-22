package utils.gs;

import java.lang.reflect.Method;
import java.nio.ByteOrder;

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

    /**
     * Used to optimize nio heap buffer bulk get operations. 'dst' must be a primitive array.
     * 'dstOffset' is measured in units of 'sizeofElements' bytes.
     */
    public static void unsafeBulkGet(Object dst, int dstOffset, int byteCount, byte[] src, int srcOffset, int sizeofElements, boolean swap) {
//    public static native void unsafeBulkGet(Object dst, int dstOffset, int byteCount,  byte[] src, int srcOffset, int sizeofElements, boolean swap);
        Refunsafe.call(memoryClass, "unsafeBulkGet", null
                , new Class[]{Object.class, int.class, int.class, byte[].class, int.class, int.class, boolean.class}
                , new Object[]{dst, dstOffset, byteCount, srcOffset, srcOffset, sizeofElements, swap});
    }

    /**
     * Used to optimize nio heap buffer bulk put operations. 'src' must be a primitive array.
     * 'srcOffset' is measured in units of 'sizeofElements' bytes.
     * @hide
     */
    public static void unsafeBulkPut(byte[] dst, int dstOffset, int byteCount, Object src, int srcOffset, int sizeofElements, boolean swap) {
        // public static native void unsafeBulkPut(byte[] dst, int dstOffset, int byteCount, Object src, int srcOffset, int sizeofElements, boolean swap);
        Method method = Refunsafe.getMethod(memoryClass, "unsafeBulkPut", byte[].class, int.class, int.class, Object.class, int.class, int.class, boolean.class);
        Refunsafe.invoke(null, method, dst, dstOffset, byteCount, src, srcOffset, sizeofElements, swap);
    }

    public static void peekBytepeekByteArray(long address, byte[] dst, int dstOffset, int byteCount) {
        //     public static native void peekByteArray(long address, byte[] dst, int dstOffset, int byteCount);
        Method method = Refunsafe.getMethod(memoryClass, "peekBytepeekByteArray"
                , long.class, byte[].class
                , int.class, int.class
        );

        Refunsafe.invoke(null, method, address, dst, dstOffset, byteCount);
    }

    public static int peekInt(byte[] src, int offset, ByteOrder order) {
        // public static int peekInt(@NonNull byte[] src, int offset, @NonNull ByteOrder order)
        Method method = Refunsafe.getMethod(memoryClass, "peekInt", byte[].class, int.class, ByteOrder.class);
        return ((Integer) Refunsafe.invoke(null, method, src, offset, order)).intValue();
    }

    public static long peekLong(byte[] src, int offset, ByteOrder order) {
        // public static long peekLong(byte[] src, int offset, ByteOrder order)
        Method method = Refunsafe.getMethod(memoryClass, "peekLong", byte[].class, int.class, ByteOrder.class);
        return ((Long) Refunsafe.invoke(null, method, src, offset, order)).longValue();
    }

    public static short peekShort(byte[] src, int offset, ByteOrder order) {
        //public static short peekShort(@NonNull byte[] src, int offset, @NonNull ByteOrder order)
        Method method = Refunsafe.getMethod(memoryClass, "peekShort", byte[].class, int.class, ByteOrder.class);
        return ((Short) Refunsafe.invoke(null, method, src, offset, order)).shortValue();
    }

    public static void pokeInt(byte[] dst, int offset, int value, ByteOrder order) {
        //public static void pokeInt(@NonNull byte[] dst, int offset, int value, @NonNull ByteOrder order)
        Method method = Refunsafe.getMethod(memoryClass, "pokeInt", byte[].class, int.class, int.class, ByteOrder.class);
        Refunsafe.invoke(null, method, dst, offset, value, order);
    }

    public static void pokeLong(byte[] dst, int offset, long value, ByteOrder order) {
        // public static void pokeLong(@NonNull byte[] dst, int offset, long value, @NonNull ByteOrder order)
        Method method = Refunsafe.getMethod(memoryClass, "pokeLong", byte[].class, int.class, long.class, ByteOrder.class);
        Refunsafe.invoke(null, method, dst, offset, value, order);
    }

    public static void pokeShort(byte[] dst, int offset, short value, ByteOrder order) {
        // public static void pokeShort(@NonNull byte[] dst, int offset, short value, @NonNull ByteOrder order)
        Method method = Refunsafe.getMethod(memoryClass, "pokeLong", byte[].class, int.class, short.class, ByteOrder.class);
        Refunsafe.invoke(null, method, dst, offset, value, order);
    }

    public static void memmove(Object dstObject, int dstOffset, Object srcObject, int srcOffset, long byteCount) {
        // public static native void memmove(@NonNull Object dstObject, int dstOffset, @NonNull Object srcObject, int srcOffset, long byteCount);
        Method method = Refunsafe.getMethod(memoryClass, "memmove", Object.class, int.class, Object.class, int.class, long.class);
        Refunsafe.invoke(null, method, dstObject, dstOffset, srcObject, srcOffset, byteCount);
    }

    public static int peekInt(long address, boolean swap) {
        // public static int peekInt(long address, boolean swap)
        Method method = Refunsafe.getMethod(memoryClass, "peekInt", long.class, boolean.class);
        return ((Integer) Refunsafe.invoke(null, method, address, swap)).intValue();
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


    public static byte[] getMethodBytes(Method method) {
        if (method == null) {
            return null;
        }
        byte[] ret = new byte[(int) ArtHelper.sizeOfArtMethod()];
        long baseAddr = ArtHelper.getMethodAddress(method);
        for (int i = 0; i < ret.length; i++) {
            ret[i] = peekByte(baseAddr + i);
        }
        return ret;
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
