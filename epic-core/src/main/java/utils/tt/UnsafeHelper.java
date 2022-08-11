package utils.tt;

import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import utils.Logger;


/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: Update unsafe
 * @Version: 1.0
 * @Create: 2022-08-09 20:40:19
 * @author: sanbo
 */
public class UnsafeHelper {
    private UnsafeHelper() {
    }

    private static Class<?> unsafeClass = null;
    private static Object unsafe1Obj = null, unsafe2Obj = null, unsafe3Obj = null, unsafe4Obj = null, unsafeObj = null;

    public static boolean isNull() {
        return unsafeObj == null;
    }

    static {
        theUnsafeField();
        if (isNull()) {
            THE_ONEField();
        }
        if (isNull()) {
            getUnsafeMethod();
        }
        if (isNull()) {
            getUnsafeByAbstractQueuedSynchronizer();
        }
    }


    public static boolean theUnsafeField() {
        try {
            if (unsafeClass == null) {
                unsafeClass = Refunsafe.findClass("sun.misc.Unsafe");
            }
            //private static final com.swift.sandhook.utils.Unsafe theUnsafe = THE_ONE;
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe1Obj = theUnsafe.get(null);
            if (unsafe1Obj != null) {
                if (unsafeObj == null) {
                    unsafeObj = unsafe1Obj;
                }
                return true;
            }
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    public static boolean THE_ONEField() {
        try {
            if (unsafeClass == null) {
                unsafeClass = Refunsafe.findClass("sun.misc.Unsafe");
            }
            //private static final com.swift.sandhook.utils.Unsafe THE_ONE = new com.swift.sandhook.utils.Unsafe();
            final Field theUnsafe = unsafeClass.getDeclaredField("THE_ONE");
            theUnsafe.setAccessible(true);
            unsafe2Obj = theUnsafe.get(null);
            if (unsafe2Obj != null) {
                if (unsafeObj == null) {
                    unsafeObj = unsafe2Obj;
                }
                return true;
            }
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    public static boolean getUnsafeMethod() {
        try {
            if (unsafeClass == null) {
                unsafeClass = Refunsafe.findClass("sun.misc.Unsafe");
            }
            final Method getUnsafe = Refunsafe.getMethod(unsafeClass, "getUnsafe");
            if (getUnsafe == null) {
                return false;
            }
            getUnsafe.setAccessible(true);
            unsafe3Obj = getUnsafe.invoke(null);
            if (unsafe3Obj != null) {
                if (unsafeObj == null) {
                    unsafeObj = unsafe3Obj;
                }
                return true;
            }
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    private static boolean getUnsafeByAbstractQueuedSynchronizer() {
        try {
            if (unsafeClass == null) {
                unsafeClass = Refunsafe.findClass("sun.misc.Unsafe");
            }
            Class AbstractQueuedSynchronizer = Refunsafe.findClass("java.util.concurrent.locks.AbstractQueuedSynchronizer");
            Field unsafeField = null;
            if (Build.VERSION.SDK_INT < 24) {
                // https://cs.android.com/android/platform/superproject/+/android-6.0.1_r9:libcore/luni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=2239
                // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r44:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=527
                // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r18:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=527
                // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=527
                unsafeField = AbstractQueuedSynchronizer.getDeclaredField("unsafe");
            } else {
                // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:libcore/luni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=499
                unsafeField = AbstractQueuedSynchronizer.getDeclaredField("U");
            }
            // 13bate 版本失效了
            // https://cs.android.com/android/platform/superproject/+/android-t-preview-2:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractOwnableSynchronizer.java
            // 但是可考虑 LockSupport.java unsafe/U来获取，获取对象转变
            if (unsafeField != null) {
                unsafeField.setAccessible(true);
                unsafe4Obj = unsafeField.get(null);
                if (unsafe4Obj != null) {
                    if (unsafeObj == null) {
                        unsafeObj = unsafe4Obj;
                    }
                    return true;
                }
            }

        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    /******************************Info[addressSize/pageSize]*****************************************************/

    public static int addressSize() {
        try {
            Method method = Refunsafe.getMethod(unsafeClass, "addressSize");
            if (method == null) {
                return 0;
            }
            // public native int addressSize();
            return (int) method.invoke(unsafeObj);
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    public static int pageSize() {
        try {
            Method method = Refunsafe.getMethod(unsafeClass, "pageSize");
            if (method == null) {
                return 0;
            }
            // public native int pageSize();
            return (int) method.invoke(unsafeObj);
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    /******************************Objects[allocateInstance/objectFieldOffset]*****************************************************/
    /**
     *  Allocates an instance of the given class without running the constructor.
     *  The class' <clinit> will be run, if necessary.
     * 实例化对象
     * @param c
     * @return
     */
    public static Object allocateInstance(Class<?> c) {
        try {
            Method method = Refunsafe.getMethod(unsafeClass, "allocateInstance", Class.class);
            if (method == null) {
                return 0;
            }
            //   public native Object allocateInstance(Class<?> c);
            return method.invoke(unsafeObj, c);

        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    /**
     * Gets the raw byte offset from the start of an object's memory to the memory used to store the indicated instance field.
     *  获取从对象内存开始到用于存储指定实例字段的内存的原始字节偏移量。
     * @param field
     * @return
     */
    public static long objectFieldOffset(Field field) {
        try {
            Method method = Refunsafe.getMethod(unsafeClass, "objectFieldOffset", Field.class);
            if (method == null) {
                return 0;
            }
            //public long objectFieldOffset(Field field) {
            return (long) method.invoke(unsafeObj, field);

        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }
    /******************************Classes[staticFieldOffset/defineClass/defineAnonymousClass/ensureClassInitialized] Has NO*****************************************************/

    /******************************Arrays[arrayBaseOffset/arrayIndexScale] *****************************************************/
    /**
     * Gets the size of each element of the given array class.
     *
     * @param clazz non-null; class in question; must be an array class
     * @return &gt; 0; the size of each element of the array
     */
    public static int arrayBaseOffset(Class clazz) {
        Method method = null;
        try {
            // public int arrayBaseOffset(Class clazz)
            method = Refunsafe.getMethod(unsafeClass, "arrayBaseOffset", Class.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method == null) {
                // private static native int getArrayBaseOffsetForComponentType(Class component_class);
                method = Refunsafe.getMethod(unsafeClass, "getArrayBaseOffsetForComponentType", Class.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                return (int) method.invoke(unsafeObj, clazz);
            }
        } catch (Throwable e) {
            e(e);
        }

        return 0;
    }

    /**
     * Gets the size of each element of the given array class.
     *
     * @param clazz non-null; class in question; must be an array class
     * @return &gt; 0; the size of each element of the array
     */
    public static int arrayIndexScale(Class clazz) {
        Method method = null;
        try {
            // public int arrayIndexScale(Class clazz)
            method = Refunsafe.getMethod(unsafeClass, "arrayIndexScale", Class.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method == null) {
                // private static native int arrayIndexScale0(Class clazz);
                method = Refunsafe.getMethod(unsafeClass, "arrayIndexScale0", Class.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method == null) {
                // private static native int getArrayIndexScaleForComponentType(Class component_class);
                method = Refunsafe.getMethod(unsafeClass, "getArrayIndexScaleForComponentType", Class.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                return (int) method.invoke(unsafeObj, clazz);
            }
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    /******************************Synchronization[monitorEnter/tryMonitorEnter/monitorExit/compareAndSwapInt/putOrderedInt]*****************************************************/
    /**
     * Performs a compare-and-set operation on an <code>int</code>
     * field within the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param expectedValue expected value of the field
     * @param newValue new value to store in the field if the contents are
     * as expected
     * @return <code>true</code> if the new value was in fact stored, and
     * <code>false</code> if not
     */
    public static boolean compareAndSwapInt(Object obj, long offset, int expectedValue, int newValue) {

        try {
            // public native boolean compareAndSwapInt(Object obj, long offset, int expectedValue, int newValue);
            Method method = Refunsafe.getMethod(unsafeClass, "compareAndSwapInt", Object.class, long.class, int.class, int.class);
            if (method == null) {
                return false;
            }
            return (boolean) method.invoke(unsafeObj, obj, offset, expectedValue, newValue);
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }



    /**
     * Performs a compare-and-set operation on a <code>long</code>
     * field within the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param expectedValue expected value of the field
     * @param newValue new value to store in the field if the contents are
     * as expected
     * @return <code>true</code> if the new value was in fact stored, and
     * <code>false</code> if not
     */
    public static boolean compareAndSwapLong(Object obj, long offset, long expectedValue, long newValue) {
        try {
            //  public native boolean compareAndSwapLong(Object obj, long offset,long expectedValue, long newValue);
            Method method = Refunsafe.getMethod(unsafeClass, "compareAndSwapLong", Object.class, long.class, long.class, long.class);
            if (method == null) {
                return false;
            }
            return (boolean) method.invoke(unsafeObj, obj, offset, expectedValue, newValue);
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    /**
     * Performs a compare-and-set operation on an <code>Object</code>
     * field (that is, a reference field) within the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param expectedValue expected value of the field
     * @param newValue new value to store in the field if the contents are
     * as expected
     * @return <code>true</code> if the new value was in fact stored, and
     * <code>false</code> if not
     */
    public static boolean compareAndSwapObject(Object obj, long offset, Object expectedValue, Object newValue) {
        try {
            // public native boolean compareAndSwapObject(Object obj, long offset, Object expectedValue, Object newValue);
            Method method = Refunsafe.getMethod(unsafeClass, "compareAndSwapObject", Object.class, long.class, Object.class, Object.class);
            if (method == null) {
                return false;
            }
            return (boolean) method.invoke(unsafeObj, obj, offset, expectedValue, newValue);
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }


    /******************************Memory[allocateMemory/copyMemory/freeMemory/getAddress/getInt/putInt]*****************************************************/

    /***********[allocateMemory]************************/

    public static long allocateMemory(long bytes) {
        try {
            // public native long allocateMemory(long bytes);
            Method method = Refunsafe.getMethod(unsafeClass, "allocateMemory", long.class);
            if (method == null) {
                return 0L;
            }
            return (long) method.invoke(unsafeObj, bytes);
        } catch (Throwable e) {
            e(e);
        }
        return 0L;
    }

    /******************************[copyMemory]************************/

    public static void copyMemoryToPrimitiveArray(long srcAddr, Object dst, long dstOffset, long bytes) {
        try {
            // public native void copyMemoryToPrimitiveArray(long srcAddr, Object dst, long dstOffset, long bytes);
            Method method = Refunsafe.getMethod(unsafeClass, "copyMemoryToPrimitiveArray", long.class, Object.class, long.class, long.class);
            if (method == null) {
                return;
            }
            method.invoke(unsafeObj, srcAddr, dst, dstOffset, bytes);
        } catch (Throwable e) {
            e(e);
        }
    }

    public static void copyMemoryFromPrimitiveArray(Object src, long srcOffset, long dstAddr, long bytes) {
        try {
            // public native void copyMemoryFromPrimitiveArray(Object src, long srcOffset, long dstAddr, long bytes);
            Method method = Refunsafe.getMethod(unsafeClass, "copyMemoryFromPrimitiveArray", Object.class, long.class, long.class, long.class);
            if (method == null) {
                return;
            }
            method.invoke(unsafeObj, src, srcOffset, dstAddr, bytes);
        } catch (Throwable e) {
            e(e);
        }
    }

    public static void copyMemory(long srcAddr, long dstAddr, long bytes) {
        try {
            // public native void copyMemory(long srcAddr, long dstAddr, long bytes);
            Method method = Refunsafe.getMethod(unsafeClass, "copyMemory", long.class, long.class, long.class);
            if (method == null) {
                return;
            }
            method.invoke(unsafeObj, srcAddr, dstAddr, bytes);
        } catch (Throwable e) {
            e(e);
        }
    }

    /******************************[setMemory]************************/
    public static void setMemory(long address, long bytes, byte value) {
        try {
            // public native void setMemory(long address, long bytes, byte value);
            Method method = Refunsafe.getMethod(unsafeClass, "setMemory", long.class, long.class, byte.class);
            if (method == null) {
                return;
            }
            method.invoke(unsafeObj, address, bytes, value);
        } catch (Throwable e) {
            e(e);
        }
    }

    /******************************[freeMemory]************************/

    public static void freeMemory(long address) {
        try {
            // public native void freeMemory(long address);
            Method method = Refunsafe.getMethod(unsafeClass, "freeMemory", long.class);
            if (method == null) {
                return;
            }
            method.invoke(unsafeObj, address);
        } catch (Throwable e) {
            e(e);
        }
    }


    /******************************[getInt]************************/
    /**
     * Gets an <code>int</code> field from the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @return the retrieved value
     */
    public static int getInt(Object obj, long offset) {
        Method method = null;
        try {
            // public native int getIntVolatile(Object obj, long offset);
            method = Refunsafe.getMethod(unsafeClass, "getIntVolatile", Object.class, long.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            // public native int getInt(Object obj, long offset);
            if (method == null) {
                method = Refunsafe.getMethod(unsafeClass, "getInt", Object.class, long.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                return (int) method.invoke(unsafeObj, obj, offset);
            }
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }


    public static int getInt(long offset) {

        Method method = null;
        try {
            // public native int getInt(long address);
            method = Refunsafe.getMethod(unsafeClass, "getInt", long.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                return (int) method.invoke(unsafeObj, offset);
            }
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

/******************************[putInt]************************/
    /**
     * Stores an <code>int</code> field into the given object.
     * @param obj
     * @param offset
     * @param newValue
     */
    public static void putInt(Object obj, long offset, int newValue) {

        Method method = null;
        try {
            // public native void putIntVolatile(Object obj, long offset, int newValue);
            method = Refunsafe.getMethod(unsafeClass, "putIntVolatile", Object.class, long.class, int.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            // public native void putInt(Object obj, long offset, int newValue);
            if (method == null) {
                method = Refunsafe.getMethod(unsafeClass, "putInt", Object.class, long.class, int.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                method.invoke(unsafeObj, obj, offset, newValue);
            }
        } catch (Throwable e) {
            e(e);
        }
    }


    public static void putInt(long address, int x) {

        Method method = null;
        try {
            // public native void putInt(long address, int x);
            method = Refunsafe.getMethod(unsafeClass, "putInt", long.class, int.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                method.invoke(unsafeObj, address, x);
            }
        } catch (Throwable e) {
            e(e);
        }
    }

    /**
     * Lazy set an int field.
     * @param obj
     * @param offset
     * @param newValue
     */
    public static void putOrderedInt(Object obj, long offset, int newValue) {
        Method method = null;
        try {
            // public native void putOrderedInt(Object obj, long offset, int newValue);
            method = Refunsafe.getMethod(unsafeClass, "putOrderedInt", Object.class, long.class, int.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                method.invoke(unsafeObj, obj, offset, newValue);
            }
        } catch (Throwable e) {
            e(e);
        }
    }
    /******************************[getLong]************************/
    /**
     * Gets a <code>long</code> field from the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @return the retrieved value
     */
    public static long getLong(Object obj, long offset) {
        Method method = null;
        try {
            // public native long getLongVolatile(Object obj, long offset);
            method = Refunsafe.getMethod(unsafeClass, "getLongVolatile", Object.class, long.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            // public native long getLong(Object obj, long offset);
            if (method == null) {
                method = Refunsafe.getMethod(unsafeClass, "getLong", Object.class, long.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                return (long) method.invoke(unsafeObj, obj, offset);
            }
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    public static long getLong(long address) {
        Method method = null;
        try {
            // public native long getLong(long address);
            method = Refunsafe.getMethod(unsafeClass, "getLong", long.class);
        } catch (Throwable e) {
            e(e);
        }

        try {
            if (method != null) {
                return (long) method.invoke(unsafeObj, address);
            }
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    /******************************[putLong]************************/

    /**
     * Stores a <code>long</code> field into the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param newValue the value to store
     */
    public static void putLong(Object obj, long offset, long newValue) {
        Method method = null;
        try {
            // public native void putLongVolatile(Object obj, long offset, long newValue);
            method = Refunsafe.getMethod(unsafeClass, "putLongVolatile", Object.class, long.class, long.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            // public native void putLong(Object obj, long offset, long newValue);
            if (method == null) {
                method = Refunsafe.getMethod(unsafeClass, "putLong", Object.class, long.class, long.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                method.invoke(unsafeObj, obj, offset, newValue);
            }
        } catch (Throwable e) {
            e(e);
        }
    }

    public static void putLong(long address, long x) {
        Method method = null;
        try {
            // public native void putLong(long address, long x);
            method = Refunsafe.getMethod(unsafeClass, "putLong", long.class, long.class);
        } catch (Throwable e) {
            e(e);
        }

        try {
            if (method != null) {
                method.invoke(unsafeObj, address, x);
            }
        } catch (Throwable e) {
            e(e);
        }
    }


    /**
     * Lazy set a long field.
     * @param obj
     * @param offset
     * @param newValue
     */
    public static void putOrderedLong(Object obj, long offset, long newValue) {
        Method method = null;
        try {
            // public native void putOrderedLong(Object obj, long offset, long newValue);
            method = Refunsafe.getMethod(unsafeClass, "putOrderedLong", Object.class, long.class, long.class);
        } catch (Throwable e) {
            e(e);
        }

        try {
            if (method != null) {
                method.invoke(unsafeObj, obj, offset, newValue);
            }
        } catch (Throwable e) {
            e(e);
        }
    }
    /******************************[getObject]************************/
    /**
     * Gets an <code>Object</code> field from the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @return the retrieved value
     */
    public static Object getObject(Object obj, long offset) {

        Method method = null;
        try {
            // public native Object getObjectVolatile(Object obj, long offset);
            method = Refunsafe.getMethod(unsafeClass, "getObjectVolatile", Object.class, long.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            // public native Object getObject(Object obj, long offset);
            if (method == null) {
                method = Refunsafe.getMethod(unsafeClass, "getObject", Object.class, long.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                return method.invoke(unsafeObj, obj, offset);
            }
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    /******************************[putObject]************************/
    /**
     * Stores an <code>Object</code> field into the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param newValue the value to store
     */
    public static void putObject(Object obj, long offset, Object newValue) {
        Method method = null;
        try {
            // public native void putObjectVolatile(Object obj, long offset,  Object newValue);
            method = Refunsafe.getMethod(unsafeClass, "putObjectVolatile", Object.class, long.class, Object.class);
        } catch (Throwable e) {
            e(e);
        }
        try {
            // public native void putObject(Object obj, long offset, Object newValue);
            if (method == null) {
                method = Refunsafe.getMethod(unsafeClass, "putObject", Object.class, long.class, Object.class);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            if (method != null) {
                method.invoke(unsafeObj, obj, offset, newValue);
            }
        } catch (Throwable e) {
            e(e);
        }
    }

    /*****************************地址&对象转换方法************************/

    //sync epic.https://github.com/SeeYouOneDayDay/epic/blob/master/epic-core/src/main/java/utils/Runtime.java#L36
    public static boolean is64Bit() {
        try {
            Method is64Bit = Refunsafe.getMethod("dalvik.system.VMRuntime", "is64Bit");
            Method getRuntime = Refunsafe.getMethod("dalvik.system.VMRuntime", "getRuntime");
            Object getRuntimeInstance = getRuntime.invoke(null);
            return (boolean) is64Bit.invoke(getRuntimeInstance);
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    public static boolean isArt() {
        try {
            return System.getProperty("java.vm.version").startsWith("2");
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    /**
     * 获取对象的内存地址
     * @param obj
     * @return
     */
    public static long toAddress(Object obj) {
        Object[] array = new Object[]{obj};
       d("toAddress() arrayIndexScale: " + arrayIndexScale(Object[].class) + "---64bit:" + is64Bit());
        //返回数组中一个元素占用的大小
        if (arrayIndexScale(Object[].class) == 8) {
            return getLong(array, arrayBaseOffset(Object[].class));
        } else {
            return 0xffffffffL & getInt(array, arrayBaseOffset(Object[].class));
        }
    }

    /**
     * 获取对应内存地址的对象
     * @param address
     * @return
     */
    public static Object fromAddress(long address) {
        Object[] array = new Object[]{null};
        long baseOffset = arrayBaseOffset(Object[].class);
        d("fromAddress() arrayIndexScale: " + arrayIndexScale(Object[].class) + "---64bit:" + is64Bit());
        if (arrayIndexScale(Object[].class) == 8) {
            putLong(array, baseOffset, address);
        } else {
            putInt(array, baseOffset, (int) address);
        }
        return array[0];
    }

    // 会崩溃！
    public static long getObjectAddress(Object o) {
        Object[] objects = {o};
        Integer baseOffset = arrayBaseOffset(Object[].class);
        if (UnsafeHelper.is64Bit()) {
            return UnsafeHelper.getLong(objects, baseOffset.longValue());
        } else {
            return UnsafeHelper.getInt(objects, baseOffset.longValue());
        }
    }

    /*****************************基础方法***********************/

    private static void d(String s) {
        Logger.d(s);
    }

    public static void e(Throwable e) {
        Logger.e(e);
    }
    /**
     *Class [sun.misc.Unsafe](http://www.docjar.com/docs/api/sun/misc/Unsafe.html) consists of `105` methods. There are, actually, few groups of important methods for manipulating with various entities. Here is some of them:
     *
     * - Info
     *     . Just returns some low-level memory information.
     *     - `addressSize`
     *     - `pageSize`
     *
     * - Objects
     *     . Provides methods for object and its fields manipulation.
     *
     *     - `allocateInstance`
     *     - `objectFieldOffset`
     *
     * - Classes
     *
     *     . Provides methods for classes and static fields manipulation.
     *
     *     - `staticFieldOffset`
     *     - `defineClass`
     *     - `defineAnonymousClass`
     *     - `ensureClassInitialized`
     *
     * - Arrays
     *
     *     . Arrays manipulation.
     *
     *     - `arrayBaseOffset`
     *     - `arrayIndexScale`
     *
     * - Synchronization
     *
     *     . Low level primitives for synchronization.
     *
     *     - `monitorEnter`
     *     - `tryMonitorEnter`
     *     - `monitorExit`
     *     - `compareAndSwapInt`
     *     - `putOrderedInt`
     *
     * - Memory
     *
     *     . Direct memory access methods.
     *
     *     - `allocateMemory`
     *     - `copyMemory`
     *     - `freeMemory`
     *     - `getAddress`
     *     - `getInt`
     *     - `putInt`
     * @return
     */
}
