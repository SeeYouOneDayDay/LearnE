package me.weishu.epic.samples.tests.custom;

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

import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    public static Class getUnsafeCalss() {
        return unsafeClass;
    }

    public static Object getUnsafeInstance() {
        return unsafeObj;
    }

    static {
        if (unsafeClass == null) {
            unsafeClass = Refunsafe.findClass("sun.misc.Unsafe");
        }
        if (theUnsafeField() || THE_ONEField() || getUnsafeMethod() || getUnsafeByAbstractQueuedSynchronizer()) {
        }
    }

    public static boolean theUnsafeField() {
        // private static final Unsafe theUnsafe = THE_ONE;
        unsafe1Obj = Refunsafe.getFieldValue(unsafeClass, "theUnsafe", null);
        return syncObj(unsafe1Obj);
    }

    public static boolean THE_ONEField() {
        //private static final com.swift.sandhook.utils.Unsafe THE_ONE = new com.swift.sandhook.utils.Unsafe();
        unsafe2Obj = Refunsafe.getFieldValue(unsafeClass, "THE_ONE", null);
        return syncObj(unsafe2Obj);
    }

    public static boolean getUnsafeMethod() {
        // public static Unsafe getUnsafe() {
        unsafe3Obj = Refunsafe.call(unsafeClass, "getUnsafe", null);
        return syncObj(unsafe3Obj);
    }

    public static boolean getUnsafeByAbstractQueuedSynchronizer() {
        // 13bate 版本失效了
        // https://cs.android.com/android/platform/superproject/+/android-t-preview-2:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractOwnableSynchronizer.java
        // 但是可考虑 LockSupport.java unsafe/U来获取，获取对象转变
        String className = "java.util.concurrent.locks.AbstractQueuedSynchronizer";
        if (Build.VERSION.SDK_INT < 24) {
            // https://cs.android.com/android/platform/superproject/+/android-6.0.1_r9:libcore/luni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=2239
            // https://cs.android.com/android/platform/superproject/+/android-10.0.0_r44:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=527
            // https://cs.android.com/android/platform/superproject/+/android-11.0.0_r18:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=527
            // https://cs.android.com/android/platform/superproject/+/android-12.0.0_r34:libcore/ojluni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=527
            unsafe4Obj = Refunsafe.getFieldValue(className, "unsafe");
            return syncObj(unsafe4Obj);
        } else if (isLargerThan12()) {
            // 12+(>31)版本，即Sv2-32、S-33以后 Unsafe调整到了VarHandle中
            //return syncObj(unsafe4Obj);
            //private static final VarHandle THREAD;
            Object TheadVarHandle = Refunsafe.getFieldValue(className, "THREAD");
            //libcore/ojluni/src/main/java/java/lang/invoke/VarHandle.java
            //private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
            unsafe4Obj = Refunsafe.getFieldValue(TheadVarHandle.getClass(), "UNSAFE");
            return syncObj(unsafe4Obj);
        } else {
            // https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:libcore/luni/src/main/java/java/util/concurrent/locks/AbstractQueuedSynchronizer.java;l=499
            unsafe4Obj = Refunsafe.getFieldValue(className, "U");
            return syncObj(unsafe4Obj);
        }
    }

    private static boolean isLargerThan12() {
        // preview版本
        try {
            if (Build.VERSION.SDK_INT == 31 && Build.VERSION.PREVIEW_SDK_INT != 0) {
                return true;
            }
        } catch (Throwable e) {
        }
        return Build.VERSION.SDK_INT > 31;
    }

    private static boolean syncObj(Object tempObj) {
        if (tempObj != null) {
            if (unsafeObj == null) {
                unsafeObj = tempObj;
            }
            return true;
        }
        return false;
    }

    /**
     * Gets the size of the address value, in bytes.
     *
     * @return the size of the address, in bytes
     */
    public static int addressSize() {
        Object result = Refunsafe.call(unsafeClass, "addressSize", unsafeObj);
        return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
    }

    /**
     * Gets the size of the memory page, in bytes.
     *
     * @return the size of the page
     */
    public static int pageSize() {
        Object result = Refunsafe.call(unsafeClass, "pageSize", unsafeObj);
        return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
    }

    /**
     *  Allocates an instance of the given class without running the constructor.
     *  The class' <clinit> will be run, if necessary.
     * 实例化对象
     * @param c
     * @return
     */
    public static Object allocateInstance(Class<?> c) {
        return Refunsafe.call(unsafeClass, "allocateInstance", unsafeObj, new Class[]{Class.class}, new Object[]{c});
    }

    /**
     * Gets the raw byte offset from the start of an object's memory to the memory used to store the indicated instance field.
     *  获取从对象内存开始到用于存储指定实例字段的内存的原始字节偏移量。
     * @param field
     * @return
     */
    public static long objectFieldOffset(Field field) {
        Object result = Refunsafe.call(unsafeClass, "objectFieldOffset", unsafeObj, new Class[]{Field.class}, new Object[]{field});
        return result != null && (result.getClass() == Long.class || result.getClass() == long.class) ? ((Long) result).longValue() : 0L;
    }

    /**
     * Gets the size of each element of the given array class.
     *
     * @param clazz non-null; class in question; must be an array class
     * @return &gt; 0; the size of each element of the array
     */
    public static int arrayBaseOffset(Class clazz) {
        Object result = Refunsafe.call(unsafeClass, "arrayBaseOffset", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
        if (result == null) {
            result = Refunsafe.call(unsafeClass, "getArrayBaseOffsetForComponentType", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
        }
        return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
    }

    /**
     * Gets the size of each element of the given array class.
     *
     * @param clazz non-null; class in question; must be an array class
     * @return &gt; 0; the size of each element of the array
     */
    public static int arrayIndexScale(Class clazz) {
        Object result = Refunsafe.call(unsafeClass, "arrayIndexScale", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
        if (result == null) {
            if (Build.VERSION.SDK_INT > 20) {
                result = Refunsafe.call(unsafeClass, "getArrayIndexScaleForComponentType", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
            } else {
                // 4.x
                result = Refunsafe.call(unsafeClass, "arrayIndexScale0", unsafeObj, new Class[]{Class.class}, new Object[]{clazz});
            }
        }
        return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
    }

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
        Object result = Refunsafe.call(unsafeClass, "compareAndSwapInt", unsafeObj
                , new Class[]{Object.class, long.class, int.class, int.class}, new Object[]{obj, offset, expectedValue, newValue});

        return result != null && (result.getClass() == Boolean.class || result.getClass() == boolean.class) ? ((Boolean) result).booleanValue() : false;

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
        Object result = Refunsafe.call(unsafeClass, "compareAndSwapLong", unsafeObj
                , new Class[]{Object.class, long.class, long.class, int.class}, new Object[]{obj, offset, expectedValue, newValue});
        return result != null && (result.getClass() == Boolean.class || result.getClass() == boolean.class) ? ((Boolean) result).booleanValue() : false;

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
        Object result = Refunsafe.call(unsafeClass, "compareAndSwapObject", unsafeObj
                , new Class[]{Object.class, long.class, Object.class, Object.class}, new Object[]{obj, offset, expectedValue, newValue});
        return result != null && (result.getClass() == Boolean.class || result.getClass() == boolean.class) ? ((Boolean) result).booleanValue() : false;
    }

    /**
     * Allocates a memory block of size {@code bytes}.
     * 分配 参数{@code bytes}大小的内存块
     * @param bytes size of the memory block 内存块的字节大小
     * @return address of the allocated memory 分配内存的地址
     */
    public static long allocateMemory(long bytes) {
        Object result = Refunsafe.call(unsafeClass, "allocateMemory", unsafeObj
                , new Class[]{long.class}, new Object[]{bytes});
        return result != null && (result.getClass() == Long.class || result.getClass() == long.class) ? ((Long) result).longValue() : 0L;
    }

    /**
     * Copies given memory block to a primitive array.
     *
     * @param srcAddr address to copy memory from
     * @param dst address to copy memory to
     * @param dstOffset offset in {@code dst}
     * @param bytes number of bytes to copy
     */
    public static void copyMemoryToPrimitiveArray(long srcAddr, Object dst, long dstOffset, long bytes) {
        Refunsafe.call(unsafeClass, "copyMemoryToPrimitiveArray", unsafeObj
                , new Class[]{long.class, Object.class, long.class, long.class}, new Object[]{srcAddr, dst, dstOffset, bytes});
    }

    /**
     * Treat given primitive array as a continuous memory block and copy it to given memory address.
     *
     * @param src primitive array to copy data from
     * @param srcOffset offset in {@code src} to copy from
     * @param dstAddr memory address to copy data to
     * @param bytes number of bytes to copy
     */
    public static void copyMemoryFromPrimitiveArray(Object src, long srcOffset, long dstAddr, long bytes) {
        Refunsafe.call(unsafeClass, "copyMemoryFromPrimitiveArray", unsafeObj
                , new Class[]{Object.class, long.class, long.class, long.class}, new Object[]{src, srcOffset, dstAddr, bytes});
    }

    /**
     * Sets all bytes in a given block of memory to a copy of another block.
     * 将给定内存块中的所有字节设置为另一个块的副本。
     * @param srcAddr address of the source memory to be copied from 要从中复制的源内存的地址
     * @param dstAddr address of the destination memory to copy to 要复制到的目标内存地址
     * @param bytes number of bytes to copy  要复制的字节数
     */
    public static void copyMemory(long srcAddr, long dstAddr, long bytes) {
        Refunsafe.call(unsafeClass, "copyMemory", unsafeObj
                , new Class[]{long.class, long.class, long.class}, new Object[]{srcAddr, dstAddr, bytes});
    }

    /**
     * Fills given memory block with a given value.
     * 用给定的值填充给定的内存块。
     * @param address address of the memoory block
     * @param bytes length of the memory block, in bytes
     * @param value fills memory with this value
     */
    public static void setMemory(long address, long bytes, byte value) {
        Refunsafe.call(unsafeClass, "setMemory", unsafeObj
                , new Class[]{long.class, long.class, byte.class}, new Object[]{address, bytes, value});
    }

    /**
     * Frees previously allocated memory at given address.
     *
     * @param address address of the freed memory
     */
    public static void freeMemory(long address) {
        Refunsafe.call(unsafeClass, "freeMemory", unsafeObj
                , new Class[]{long.class}, new Object[]{address});
    }

    /**
     * Gets an <code>int</code> field from the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @return the retrieved value
     */
    public static int getInt(Object obj, long offset) {
        Object result = Refunsafe.call(unsafeClass, "getIntVolatile", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        if (result == null) {
            result = Refunsafe.call(unsafeClass, "getInt", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        }
        return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
    }



    public static int getInt(long offset) {
        Object result = Refunsafe.call(unsafeClass, "getInt", unsafeObj, new Class[]{long.class}, new Object[]{offset});
        return result != null && (result.getClass() == Integer.class || result.getClass() == int.class) ? ((Integer) result).intValue() : 0;
    }

    /**
     * Stores an <code>int</code> field into the given object.
     * @param obj
     * @param offset
     * @param newValue
     */
    public static void putInt(Object obj, long offset, int newValue) {
        Method method = Refunsafe.getMethod(unsafeClass, "putIntVolatile", Object.class, long.class, int.class);
        if (method == null) {
            method = Refunsafe.getMethod(unsafeClass, "putInt", Object.class, long.class, int.class);
        }
        if (method != null) {
            method.setAccessible(true);
            Refunsafe.invoke(unsafeObj, method, obj, offset, newValue);
        }
    }

    /**
     * Stores a {@code int} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    public static void putInt(long address, int newValue) {
        Refunsafe.call(unsafeClass, "getInt", unsafeObj, new Class[]{long.class, int.class}, new Object[]{address, newValue});
    }

    /**
     * Lazy set an int field.
     * @param obj
     * @param offset
     * @param newValue
     */
    public static void putOrderedInt(Object obj, long offset, int newValue) {
        Refunsafe.call(unsafeClass, "putOrderedInt", unsafeObj, new Class[]{Object.class, long.class, int.class}, new Object[]{obj, offset, newValue});
    }

    /**
     * Gets a <code>long</code> field from the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @return the retrieved value
     */
    public static long getLong(Object obj, long offset) {
        Object result = Refunsafe.call(unsafeClass, "getLongVolatile", unsafeObj
                , new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        if (result == null) {
            result = Refunsafe.call(unsafeClass, "getLong", unsafeObj
                    , new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        }
        return result != null && (result.getClass() == Long.class || result.getClass() == long.class) ? ((Long) result).longValue() : 0L;
    }

    /**
     * Gets {@code long} from given address in memory.
     *
     * @param address address in memory
     * @return {@code long} value
     */
    public static long getLong(long address) {
        Object result = Refunsafe.call(unsafeClass, "getLong", unsafeObj, new Class[]{long.class}, new Object[]{address});
        return result != null && (result.getClass() == Long.class || result.getClass() == long.class) ? ((Long) result).longValue() : 0L;
    }

    /**
     * Stores a <code>long</code> field into the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param newValue the value to store
     */
    public static void putLong(Object obj, long offset, long newValue) {
        Method method = Refunsafe.getMethod(unsafeClass, "putLongVolatile", Object.class, long.class, long.class);
        if (method == null) {
            method = Refunsafe.getMethod(unsafeClass, "putLong", Object.class, long.class, long.class);
        }
        if (method != null) {
            method.setAccessible(true);
            Refunsafe.invoke(unsafeObj, method, obj, offset, newValue);
        }
    }

    /**
     * Stores a {@code long} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    public static void putLong(long address, long newValue) {
        Refunsafe.call(unsafeClass, "putLong", unsafeObj
                , new Class[]{long.class, long.class}, new Object[]{address, newValue});
    }


    /**
     * Lazy set a long field.
     * @param obj
     * @param offset
     * @param newValue
     */
    public static void putOrderedLong(Object obj, long offset, long newValue) {
        Refunsafe.call(unsafeClass, "putOrderedLong", unsafeObj
                , new Class[]{Object.class, long.class, long.class}, new Object[]{obj, offset, newValue});
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
        Object result = Refunsafe.call(unsafeClass, "getObjectVolatile", unsafeObj
                , new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        if (result == null) {
            result = Refunsafe.call(unsafeClass, "getObject", unsafeObj
                    , new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        }
        return result;
    }

    /**
     * Gets a {@code byte} field from the given object.
     *
     * @param obj non-{@code null}; object containing byte field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    public static byte getByte(Object obj, long offset) {
        Object result = Refunsafe.call(unsafeClass, "getByte", unsafeObj
                , new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        return result != null && (result.getClass() == Byte.class || result.getClass() == byte.class) ? ((Byte) result).byteValue() : 0;
    }

    /**
     * Gets {@code byte} from given address in memory.
     *
     * @param address address in memory
     * @return {@code byte} value
     */
    public static byte getByte(long address) {
        Object result = Refunsafe.call(unsafeClass, "getByte", unsafeObj
                , new Class[]{long.class}, new Object[]{address});
        return result != null && (result.getClass() == Byte.class || result.getClass() == byte.class) ? ((Byte) result).byteValue() : 0;
    }

    /**
     * Stores a {@code byte} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    public static void putByte(long address, byte newValue) {
        Refunsafe.call(unsafeClass, "putByte", unsafeObj
                , new Class[]{long.class, byte.class}, new Object[]{address, newValue});
    }

    /**
     * Stores a {@code byte} field into the given object.
     *
     * @param obj non-{@code null}; object containing byte field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    public static void putByte(Object obj, long offset, byte newValue) {
        Refunsafe.call(unsafeClass, "putByte", unsafeObj
                , new Class[]{Object.class, long.class, byte.class}, new Object[]{obj, offset, newValue});
    }

    /**
     * Gets a {@code boolean} field from the given object.
     *
     * @param obj non-{@code null}; object containing boolean field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    public static boolean getBoolean(Object obj, long offset) {
        Object result = Refunsafe.call(unsafeClass, "getBoolean", unsafeObj
                , new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        return result != null && (result.getClass() == Boolean.class || result.getClass() == boolean.class) ? ((Boolean) result).booleanValue() : false;
    }

    /**
     * Stores a {@code boolean} field into the given object.
     *
     * @param obj non-{@code null}; object containing boolean field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    public static void putBoolean(Object obj, long offset, boolean newValue) {
        Refunsafe.call(unsafeClass, "putBoolean", unsafeObj
                , new Class[]{Object.class, long.class, boolean.class}, new Object[]{obj, offset, newValue});
    }

    /**
     * Gets a {@code float} field from the given object.
     *
     * @param obj non-{@code null}; object containing float field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    public static float getFloat(Object obj, long offset) {
        Object result = Refunsafe.call(unsafeClass, "getFloat", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        return result != null && (result.getClass() == Float.class || result.getClass() == float.class) ? ((Float) result).floatValue() : 0F;
    }

    /**
     * Gets {@code long} from given address in memory.
     *
     * @param address address in memory
     * @return {@code long} value
     */
    public static float getFloat(long address) {
        Object result = Refunsafe.call(unsafeClass, "getFloat", unsafeObj, new Class[]{long.class}, new Object[]{address});
        return result != null && (result.getClass() == Float.class || result.getClass() == float.class) ? ((Float) result).floatValue() : 0F;
    }

    /**
     * Stores a {@code float} field into the given object.
     *
     * @param obj non-{@code null}; object containing float field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    public static void putFloat(Object obj, long offset, float newValue) {
        Refunsafe.call(unsafeClass, "getFloat", unsafeObj, new Class[]{Object.class, long.class, float.class}, new Object[]{obj, offset, newValue});
    }

    /**
     * Stores a {@code float} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */

    public static void putFloat(long address, float newValue) {
        Refunsafe.call(unsafeClass, "putFloat", unsafeObj, new Class[]{long.class, float.class}, new Object[]{address, newValue});
    }

    /**
     * Gets a {@code double} field from the given object.
     *
     * @param obj non-{@code null}; object containing double field
     * @param offset offset to the field within {@code obj}
     * @return the retrieved value
     */
    public static double getDouble(Object obj, long offset) {
        Object result = Refunsafe.call(unsafeClass, "getDouble", unsafeObj, new Class[]{Object.class, long.class}, new Object[]{obj, offset});
        return result != null && (result.getClass() == Double.class || result.getClass() == double.class) ? ((Double) result).doubleValue() : 0D;
    }

    /**
     * Gets {@code double} from given address in memory.
     *
     * @param address address in memory
     * @return {@code double} value
     */
    public static double getDouble(long address) {
        Object result = Refunsafe.call(unsafeClass, "getDouble", unsafeObj, new Class[]{long.class}, new Object[]{address});
        return result != null && (result.getClass() == Double.class || result.getClass() == double.class) ? ((Double) result).doubleValue() : 0D;
    }

    /**
     * Stores a {@code double} into the given memory address.
     *
     * @param address address in memory where to store the value
     * @param newValue the value to store
     */
    public static void putDouble(long address, double newValue) {
        Refunsafe.call(unsafeClass, "putDouble", unsafeObj, new Class[]{long.class, double.class}, new Object[]{address, newValue});
    }


    /**
     * Stores a {@code double} field into the given object.
     *
     * @param obj non-{@code null}; object containing double field
     * @param offset offset to the field within {@code obj}
     * @param newValue the value to store
     */
    public static void putDouble(Object obj, long offset, double newValue) {
        Refunsafe.call(unsafeClass, "putDouble", unsafeObj, new Class[]{Object.class, long.class, double.class}, new Object[]{obj, offset, newValue});
    }

    /**
     * Stores an <code>Object</code> field into the given object.
     *
     * @param obj non-null; object containing the field
     * @param offset offset to the field within <code>obj</code>
     * @param newValue the value to store
     */
    public static void putObject(Object obj, long offset, Object newValue) {
        Method method = Refunsafe.getMethod(unsafeClass, "putObjectVolatile", Object.class, long.class, Object.class);
        if (method == null) {
            method = Refunsafe.getMethod(unsafeClass, "putObject", Object.class, long.class, Object.class);
        }
        if (method != null) {
            method.setAccessible(true);
            Refunsafe.invoke(unsafeObj, method, obj, offset, newValue);
        }
    }

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    public static final int getAndAddInt(Object o, long offset, int delta) {
//        // 貌似安卓7+才有
//        // 原型: public final int getAndAddInt(Object o, long offset, int delta)
//        Method method = Refunsafe.getMethod(unsafeClass, "getAndAddInt", Object.class, long.class, int.class);
//        return (int) Refunsafe.invoke(unsafeObj, method, o, offset, delta);
        //内部实现
        int v;
        do {
            v = getInt(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    public static final long getAndAddLong(Object o, long offset, long delta) {
//        // 貌似安卓7+才有
//        // 原型: public final long getAndAddLong(Object o, long offset, long delta)
//        Method method = Refunsafe.getMethod(unsafeClass, "getAndAddLong", Object.class, long.class, long.class);
//        return (long) Refunsafe.invoke(unsafeObj, method, o, offset, delta);
        // 内部实现
        long v;
        do {
            v = getLong(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    public static final int getAndSetInt(Object o, long offset, int newValue) {
//        // 貌似安卓7+才有
//        // 原型: public final int getAndSetInt(Object o, long offset, int newValue)
//        Method method = Refunsafe.getMethod(unsafeClass, "getAndSetInt", Object.class, long.class, int.class);
//        return (int) Refunsafe.invoke(unsafeObj, method, o, offset, newValue);
        // 内部实现
        int v;
        do {
            v = getInt(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object {@code o}
     * at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    public static final long getAndSetLong(Object o, long offset, long newValue) {
//        // 貌似安卓7+才有
//        // 原型: public final long getAndSetLong(Object o, long offset, long newValue)
//        Method method = Refunsafe.getMethod(unsafeClass, "getAndSetLong", Object.class, long.class, long.class);
//        return (long) Refunsafe.invoke(unsafeObj, method, o, offset, newValue);
        // 内部实现
        long v;
        do {
            v = getLong(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object {@code o} at the given {@code offset}.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    public static final Object getAndSetObject(Object o, long offset, Object newValue) {
//        // 貌似安卓7+才有
//        // 原型: public final Object getAndSetObject(Object o, long offset, Object newValue)
//        Method method = Refunsafe.getMethod(unsafeClass, "getAndSetObject", Object.class, long.class, Object.class);
//        return Refunsafe.invoke(unsafeObj, method, o, offset, newValue);
        // 内部实现
        Object v;
        do {
            v = getObject(o, offset);
        } while (!compareAndSwapObject(o, offset, v, newValue));
        return v;
    }


    /*****************************地址&对象转换方法************************/

    //sync epic.https://github.com/SeeYouOneDayDay/epic/blob/master/epic-core/src/main/java/utils/Runtime.java#L36
    public static boolean is64Bit() {
        try {
            Object getRuntimeInstance = Refunsafe.call("dalvik.system.VMRuntime", "getRuntime", null);
            Object is64BitInstance = Refunsafe.call("dalvik.system.VMRuntime", "is64Bit", getRuntimeInstance);
            return is64BitInstance != null && (is64BitInstance.getClass() == Boolean.class || is64BitInstance.getClass() == boolean.class)
                    ? ((Boolean) is64BitInstance).booleanValue() : false;
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



    /*****************************基础方法***********************/

    public static void d(String s) {
    }

    public static void i(String s) {
    }

    public static void e(String s) {
    }

    public static void e(Throwable e) {
    }
}
