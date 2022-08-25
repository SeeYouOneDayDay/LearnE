package utils.gs;

import java.lang.reflect.Method;

import utils.Logger;

public class Aho {
    public static byte[] getMethodBytes(Method method) {
        if (method == null) {
            return null;
        }
        byte[] ret = new byte[(int) ArtHelper.sizeOfArtMethod()];
        long baseAddr = ArtHelper.getMethodAddress(method);
        for (int i = 0; i < ret.length; i++) {
            ret[i] = MemoryHelper.peekByte(baseAddr + i);
        }
        return ret;
    }

    // 可替代 memput(bytes, dest)
    public static void memput(byte[] bytes, long dst) {
        for (int i = 0; i < bytes.length; i++) {
            MemoryHelper.pokeByte(dst, bytes[i]);
            dst++;
        }
    }

    //  memget(src, length)  可读取内存数据
    public static byte[] memget(long srcAddress, int length) {
        long dstBuf = UnsafeHelper.allocateMemory(length);
        UnsafeHelper.copyMemory(srcAddress, dstBuf, length);
        byte[] dst = new byte[length];
        for (int i = 0; i < length; ++i) {
            byte srcByte = UnsafeHelper.getByte(srcAddress++);
            byte dstByte = UnsafeHelper.getByte(dstBuf++);
            if (srcByte != dstByte) {
                Logger.e(String.format("UnsafeHelper copy Failed!  offset %d: src = '%c', dst = '%c'",
                        i, srcByte, dstByte));
            }
            dst[i] = srcByte;
        }
//        UnsafeHelper.freeMemory(dstBuf);
        return dst;
    }


    public static long mmap(int length) {
        Object os = Refunsafe.getFieldValue("libcore.io.Libcore", "rawOs");
        //    public long mmap(long address, long byteCount, int prot, int flags, FileDescriptor fd, long offset) throws ErrnoException;
        //mmap(0, (size_t) length, PROT_READ | PROT_WRITE | PROT_EXEC, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        int prot = 0x1 | 0x2 | 0x4;
        int flags = 0x02 | 0x20;
        long address = (long) Refunsafe.call("libcore.io.Os", "mmap", os
                , new Class[]{long.class, long.class, int.class, int.class, java.io.FileDescriptor.class, long.class}
                , new Object[]{0, length, prot, flags, new java.io.FileDescriptor(), 0}
        );
        return address;
    }

    public static boolean munmap(long address, int length) {
        //libcore/luni/src/main/java/libcore/io/Libcore.java
        Object os = Refunsafe.getFieldValue("libcore.io.Libcore", "rawOs");
        // public void munmap(long address, long byteCount) throws ErrnoException;
        Refunsafe.call("libcore.io.Os", "munmap", os
                , new Class[]{long.class, long.class}
                , new Object[]{address, length}
        );
        return true;
    }


    public static boolean unmap(long address, int length) {
        try {
            UnsafeHelper.setMemory(address, length, (byte) 0);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

}
