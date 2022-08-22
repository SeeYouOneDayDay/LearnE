/*
 * Copyright (c) 2017, weishu twsxtd@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.weishu.epic.art;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedHelpers;
import utils.DeviceCheck;
import utils.Logger;
import utils.gs.UnsafeHelper;


public final class EpicNative {

    private static final String TAG = "JEpicNative";
    private static volatile boolean useUnsafe = false;

    public static boolean isUseUnsafe() {
        return useUnsafe;
    }

    static {
        try {
            System.loadLibrary("epic");
            useUnsafe = DeviceCheck.isYunOS() || !isGetObjectAvailable();
            Logger.i(TAG, "use unsafe ? " + useUnsafe);
        } catch (Throwable e) {
            Logger.e(TAG, "init EpicNative error", e);
        }
    }

    public static native long mmap(int length);

    public static native boolean munmap(long address, int length);

    public static native void memcpy(long src, long dest, int length);

    public static native void memput(byte[] bytes, long dest);

    public static native byte[] memget(long src, int length);

    public static native boolean munprotect(long addr, long len);


    public static native void MakeInitializedClassVisibilyInitialized(long self);

    public static native boolean cacheflush(long addr, long len);

    public static native long malloc(int sizeOfPtr);

    public static native Object getObjectNative(long self, long address);

    private static native boolean isGetObjectAvailable();

    public static native long getMethodAddress(Member method);



    public static native boolean compileMethod(Member method, long self);


    /**
     * 在Hook的过程中暂停所有其他线程，不让它们有机会修改代码；在Hook完毕之后在恢复执行。
     *      那么问题来了，如何暂停/恢复所有线程？Google了一番发现有人通过ptrace实现：
     *      开一个linux task然后挨个ptrace本进程内的所有子线程，这样就是实现了暂停。
     *      这种方式很重而且不是特别稳定，于是我就放弃了。ART虚拟机内部一定也有暂停线程的需求（比如GC），
     *      因此我可以选择直接调用ART的内部函数。
     *
     * 在源码里面捞了一番之后果然在thread_list.cc 中找到了这样的函数 resumeAll/suspendAll；
     *      不过遗憾的是这两个函数是ThreadList类的成员函数，要调用他们必须拿到ThreadList的指针；
     *      一般情况下是没有比较稳定的方式拿到这个对象的。不过好在Android 源码通过RAII机制对 suspendAll/resumeAll做了一个封装，
     *      名为 ScopedSuspendAll 这类的构造函数里面执行暂停操作，析构函数执行恢复操作，
     *      在栈上分配变量此类型的变量之后，在这个变量的作用域内可以自动实现暂停和恢复。
     *      因此我只需要用 dlsym 拿到构造函数和析构函数的符号之后，直接调用就能实现暂停恢复功能
     */
    /**
     * suspend all running thread momently
     * @return a handle to resume all thread, used by {@link #resumeAll(long)}
     */
    public static native long suspendAll();

    /**
     * resume all thread which are suspend by {@link #suspendAll()}
     * only work abobe Android N
     * @param cookie the cookie return by {@link #suspendAll()}
     */
    public static native void resumeAll(long cookie);

    /**
     * stop jit compiler in runtime.
     * Warning: Just for experiment Do not call this now!!!
     * @return cookie use by {@link #startJit(long)}
     */
    public static native long stopJit();

    /**
     * start jit compiler stop by {@link #stopJit()}
     * Warning: Just for experiment Do not call this now!!!
     * @param cookie the cookie return by {@link #stopJit()}
     */
    public static native void startJit(long cookie);

    // FIXME: 17/12/29 reimplement it with pure native code.
    static native boolean activateNative(long jumpToAddress, long pc, long sizeOfTargetJump, long sizeOfBridgeJump, byte[] code);

    /**
     * Disable the moving gc of runtime.
     * Warning: Just for experiment Do not call this now!!!
     * @param api the api level
     */
    public static native void disableMovingGc(int api);


    private EpicNative() {
    }


    public static Object getObject(long self, long address) {
        Logger.e(TAG, "getObject============"+useUnsafe
                +"-------"
                +"\r\n\tnative: "+getObjectNative(self, address)
                +"\r\n\tplan2: "+ UnsafeHelper.fromAddress(address)
        );
        if (useUnsafe) {
            Logger.d(TAG, "使用Unsafe方式获取对象");
            //必然崩溃
//            return UnsafeHelper.getObject(address);
            return UnsafeHelper.fromAddress(address);
        } else {
            Logger.d(TAG, "使用native方式获取对象");
            return getObjectNative(self, address);
        }
    }
    /**
     *  Thread
     *      private volatile long nativePeer;
     *  对本机线程对象的引用。
     *  如果本机线程尚未创建/启动或已被销毁，则为 0。
     * @param method
     * @return
     */

    public static boolean compileMethod(Member method) {
        final long nativePeer = XposedHelpers.getLongField(Thread.currentThread(), "nativePeer");
        return compileMethod(method, nativePeer);
    }

    //通过地址获取对象
    public static Object getObject(long address) {
        final long nativePeer = XposedHelpers.getLongField(Thread.currentThread(), "nativePeer");
        return getObject(nativePeer, address);
    }

    //https://android.googlesource.com/platform/art/+/master/runtime/class_linker.cc#319
    //void ClassLinker::MakeInitializedClassesVisiblyInitialized(Thread* self, bool wait)
    public static void MakeInitializedClassVisibilyInitialized() {
        final long nativePeer = XposedHelpers.getLongField(Thread.currentThread(), "nativePeer");
        MakeInitializedClassVisibilyInitialized(nativePeer);
    }

    public static long map(int length) {
        long addr = mmap(length);
//        Logger.i(TAG, "Mapped memory of size " + length + " at " + addrHex(m));

////        // 替代测试成功
//        long addr1 = UnsafeHelper.mmap(length);
//        byte[] bbs = UnsafeHelper.getData(addr, length);
//        byte[] bbs1 = UnsafeHelper.getData(addr, length);
//        Logger.d("------------map------------测试结果对比."
//                + "\r\n\tnative memget 结果: " + Debug.hexdump(bbs, addr)
//                + "\r\n\tUnsafe copy() 结果: " + Debug.hexdump(bbs1, addr1)
//        );
        return addr;
    }


    public static boolean unmap(long address, int length) {
//        Logger.d(TAG, "Removing mapped memory of size " + length + " at " + addrHex(address));
        return munmap(address, length);
        // 可使用如放方法替代
//        return UnsafeHelper.unmap(address, length);
//        return UnsafeHelper.munmap(address, length);

//        //https://cs.android.com/android/platform/superproject/+/master:libcore/ojluni/src/main/java/sun/nio/ch/Util.java
//        //static void erase(ByteBuffer bb) {
//        // 可以可用这个方法擦除 unsafe.setMemory(((DirectBuffer)bb).address(), bb.capacity(), (byte)0);
//
//        // 貌似这个方法可以哎
//          UnsafeHelper.setMemory(address, length, (byte) 0);
//         return true;
    }

    public static void put(byte[] bytes, long dest) {
//        Logger.d(TAG, "put() Writing memory to: " + addrHex(dest));
//        Logger.d(TAG, "put()  bytes: :" + Debug.hexdump(bytes, dest));
        memput(bytes, dest);
        //可使用  UnsafeHelper.setData(bytes, dest);替代

//        Logger.d("------------put------------测试.");
//        UnsafeHelper.setData(bytes, dest);
//
//        byte[] b1 = memget(dest, bytes.length);
//        // 可以用该值替代
//        byte[] bbs2 = UnsafeHelper.getData(dest, bytes.length);
//        Logger.d("------------put------------测试结果对比."
//                        + "\r\n\tnative memget 结果: " + Debug.hexdump(b1, dest)
//                        + "\r\n\tUnsafe copy() 结果: " + Debug.hexdump(bbs2, dest)
//        );
    }

    public static byte[] get(long src, int length) {
//        Logger.d(TAG, "get函数  Reading(length): " + length + " bytes from: " + src + "--->" + addrHex(src));
        byte[] bytes = memget(src, length);
//        Logger.d(TAG, "——————————get函数 memget 结果:" + Debug.hexdump(bytes, src));
//
//        // 可以用该值替代
        byte[] bbs = UnsafeHelper.getData(src, length);
//        Logger.d("——————[可替代]————get函数结果对比."
////                +"\r\n\tnative memget 结果: " + Debug.getString(bytes)
////                +"\r\n\tUnsafe copy() 结果:" + Debug.getString(bbs)
//                        + "\r\n\tnative memget 结果: " + Debug.hexdump(bytes, src)
//                        + "\r\n\tUnsafe copy() 结果: " + Debug.hexdump(bbs, src)
//        );

        return bbs;
    }

    public static boolean unprotect(long addr, long len) {
//        Logger.d(TAG, "Disabling mprotect from " + addrHex(addr));
        return munprotect(addr, len);
    }

    public static void copy(long src, long dst, int length) {
//        Logger.d(TAG, "Copy " + length + " bytes form " + addrHex(src) + " to " + addrHex(dst));
        memcpy(src, dst, length);
        // 可考虑替换
//        MemoryHelper.memcpy(dst,src,length);
    }

}



