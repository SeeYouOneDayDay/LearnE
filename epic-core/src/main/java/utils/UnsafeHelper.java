package utils;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;

import utils.Logger;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: TODO
 * @Version: 1.0
 * @Create: 2022/08/214 17:56:41
 * @author: sanbo
 */
public class UnsafeHelper {
    /******************************************************************/

    private static Class<?> unsafeClass = null;
    private static Object unsafe1 = null, unsafe2 = null, unsafe3 = null, unsafe = null;

    public static boolean isNull() {
        return unsafe == null;
    }

    static {
        theUnsafeField();
        if (isNull()) {
            THE_ONEField();
        }
        if (isNull()) {
            getUnsafeMethod();
        }
    }

    public static boolean theUnsafeField() {
        try {
            if (unsafeClass == null) {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            //private static final com.swift.sandhook.utils.Unsafe theUnsafe = THE_ONE;
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe1 = theUnsafe.get(null);
            if (unsafe1 != null) {
                if (unsafe == null) {
                    unsafe = unsafe1;
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
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            //private static final com.swift.sandhook.utils.Unsafe THE_ONE = new com.swift.sandhook.utils.Unsafe();
            final Field theUnsafe = unsafeClass.getDeclaredField("THE_ONE");
            theUnsafe.setAccessible(true);
            unsafe2 = theUnsafe.get(null);
            if (unsafe2 != null) {
                if (unsafe == null) {
                    unsafe = unsafe2;
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
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            //public static com.swift.sandhook.utils.Unsafe getUnsafe()
            final Method getUnsafe = unsafeClass.getDeclaredMethod("getUnsafe");
            getUnsafe.setAccessible(true);
            unsafe3 = getUnsafe.invoke(null);
            if (unsafe3 != null) {
                if (unsafe == null) {
                    unsafe = unsafe3;
                }
                return true;
            }
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }

    /******************************Info[addressSize/pageSize]*****************************************************/

    public static int addressSize() {
        try {
            // public native int addressSize();
            return (int) unsafeClass.getDeclaredMethod("addressSize").invoke(unsafe);
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    public static int pageSize() {
        try {
            // public native int pageSize();
            return (int) unsafeClass.getDeclaredMethod("pageSize").invoke(unsafe);
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
            //   public native Object allocateInstance(Class<?> c);
            return unsafeClass.getDeclaredMethod("allocateInstance", Class.class).invoke(unsafe, c);

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
            //public long objectFieldOffset(Field field) {
            return (long) unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, field);

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
        try {
            //public int arrayBaseOffset(Class clazz)
            return (int) unsafeClass.getDeclaredMethod("arrayBaseOffset", Class.class).invoke(unsafe, clazz);
        } catch (Throwable e) {
            e(e);
            try {
                //private static native int getArrayBaseOffsetForComponentType(Class component_class);
                return (int) unsafeClass.getDeclaredMethod("getArrayBaseOffsetForComponentType", Class.class).invoke(unsafe, clazz);
            } catch (Throwable e2) {
                e(e2);
            }
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
        try {
            //public int arrayIndexScale(Class clazz)
            return (int) unsafeClass.getDeclaredMethod("arrayIndexScale", Class.class).invoke(unsafe, clazz);
        } catch (Throwable e) {
            e(e);
            try {
                // private static native int arrayIndexScale0(Class clazz);
                return (int) unsafeClass.getDeclaredMethod("arrayIndexScale0", Class.class).invoke(unsafe, clazz);
            } catch (Throwable e2) {
                e(e2);
                try {
                    // private static native int getArrayIndexScaleForComponentType(Class component_class);
                    return (int) unsafeClass.getDeclaredMethod("getArrayIndexScaleForComponentType", Class.class).invoke(unsafe, clazz);
                } catch (Throwable e3) {
                    e(e3);
                }
            }
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
            //public native boolean compareAndSwapInt(Object obj, long offset, int expectedValue, int newValue);
            return (boolean) unsafeClass.getDeclaredMethod("compareAndSwapInt", Object.class, long.class, int.class, int.class).invoke(unsafe, obj, offset, expectedValue, newValue);
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
            // public native boolean compareAndSwapLong(Object obj, long offset,long expectedValue, long newValue);
            return (boolean) unsafeClass.getDeclaredMethod("compareAndSwapLong", Object.class, long.class, long.class, long.class).invoke(unsafe, obj, offset, expectedValue, newValue);
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
            return (boolean) unsafeClass.getDeclaredMethod("compareAndSwapObject", Object.class, long.class, Object.class, Object.class).invoke(unsafe, obj, offset, expectedValue, newValue);
        } catch (Throwable e) {
            e(e);
        }
        return false;
    }


    /******************************Memory[allocateMemory/copyMemory/freeMemory/getAddress/getInt/putInt]*****************************************************/

    /***********[allocateMemory]************************/

    public static long allocateMemory(long bytes) {
        try {
            //public native long allocateMemory(long bytes);
            return (long) unsafeClass.getDeclaredMethod("allocateMemory", long.class).invoke(unsafe, bytes);
        } catch (Throwable e) {
            e(e);
        }
        return 0;
    }

    /******************************[copyMemory]************************/

    public static void copyMemoryToPrimitiveArray(long srcAddr, Object dst, long dstOffset, long bytes) {
        try {
            //public native void copyMemoryToPrimitiveArray(long srcAddr, Object dst, long dstOffset, long bytes);
            unsafeClass.getDeclaredMethod("copyMemoryToPrimitiveArray", long.class, Object.class, long.class, long.class).invoke(unsafe, srcAddr, dst, dstOffset, bytes);
        } catch (Throwable e) {
            e(e);
        }
    }

    public static void copyMemoryFromPrimitiveArray(Object src, long srcOffset, long dstAddr, long bytes) {
        try {
            //public native void copyMemoryFromPrimitiveArray(Object src, long srcOffset, long dstAddr, long bytes);
            unsafeClass.getDeclaredMethod("copyMemoryFromPrimitiveArray", Object.class, long.class, long.class, long.class).invoke(unsafe, src, srcOffset, dstAddr, bytes);
        } catch (Throwable e) {
            e(e);
        }
    }

    public static void copyMemory(long srcAddr, long dstAddr, long bytes) {
        try {
            //public native void copyMemory(long srcAddr, long dstAddr, long bytes);
            unsafeClass.getDeclaredMethod("copyMemory", long.class, long.class, long.class).invoke(unsafe, srcAddr, dstAddr, bytes);
        } catch (Throwable e) {
            e(e);
        }
    }

    /******************************[setMemory]************************/
    public static void setMemory(long address, long bytes, byte value) {
        try {
            //public native void setMemory(long address, long bytes, byte value);
            unsafeClass.getDeclaredMethod("setMemory", long.class, long.class, byte.class).invoke(unsafe, address, bytes, value);
        } catch (Throwable e) {
            e(e);
        }
    }

    /******************************[freeMemory]************************/

    public static void freeMemory(long address) {
        try {
            //public native void freeMemory(long address);
            unsafeClass.getDeclaredMethod("freeMemory", long.class).invoke(unsafe, address);
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
        try {
            // public native int getIntVolatile(Object obj, long offset);
            return (int) unsafeClass.getDeclaredMethod("getIntVolatile", Object.class, long.class).invoke(unsafe, obj, offset);
        } catch (Throwable e) {
            e(e);
            try {
                // public native int getInt(Object obj, long offset);
                return (int) unsafeClass.getDeclaredMethod("getInt", Object.class, long.class).invoke(unsafe, obj, offset);
            } catch (Throwable e1) {
                e(e1);
            }
        }
        return 0;
    }


    public static int getInt(long offset) {
        try {
            // public native int getInt(long address);
            return (int) unsafeClass.getDeclaredMethod("getInt", long.class).invoke(unsafe, offset);
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
        try {
            // public native void putIntVolatile(Object obj, long offset, int newValue);
            unsafeClass.getDeclaredMethod("putIntVolatile", Object.class, long.class, int.class).invoke(unsafe, obj, offset, newValue);
        } catch (Throwable e) {
            e(e);
            try {
                // public native void putInt(Object obj, long offset, int newValue);
                unsafeClass.getDeclaredMethod("putInt", Object.class, long.class, int.class).invoke(unsafe, obj, offset, newValue);
            } catch (Throwable e1) {
                e(e1);
            }
        }
    }


    public static void putInt(long address, int x) {
        try {
            //  public native void putInt(long address, int x);
            unsafeClass.getDeclaredMethod("putInt", long.class, int.class).invoke(unsafe, address, x);
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
        try {
            //public native void putOrderedInt(Object obj, long offset, int newValue);
            unsafeClass.getDeclaredMethod("putOrderedInt", Object.class, long.class, int.class).invoke(unsafe, obj, offset, newValue);
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
        try {
            // public native long getLongVolatile(Object obj, long offset);
            return (long) unsafeClass.getDeclaredMethod("getLongVolatile", Object.class, long.class).invoke(unsafe, obj, offset);
        } catch (Throwable e) {
            e(e);
            try {
                // public native long getLong(Object obj, long offset);
                return (long) unsafeClass.getDeclaredMethod("getLong", Object.class, long.class).invoke(unsafe, obj, offset);
            } catch (Throwable e1) {
                e(e1);
            }
        }
        return 0;
    }

    public static long getLong(long address) {
        try {
            //  public native long getLong(long address);
            return (long) unsafeClass.getDeclaredMethod("getLong", long.class).invoke(unsafe, address);
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
        try {
            // public native void putLongVolatile(Object obj, long offset, long newValue);
            unsafeClass.getDeclaredMethod("putLongVolatile", Object.class, long.class, long.class).invoke(unsafe, obj, offset, newValue);
        } catch (Throwable e) {
            e(e);
            try {
                // public native void putLong(Object obj, long offset, long newValue);
                unsafeClass.getDeclaredMethod("putLong", Object.class, long.class, long.class).invoke(unsafe, obj, offset, newValue);
            } catch (Throwable e1) {
                e(e1);
            }
        }
    }

    public static void putLong(long address, long x) {
        try {
            // public native void putLong(long address, long x);
            unsafeClass.getDeclaredMethod("putLong", long.class, long.class).invoke(unsafe, address, x);
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
        try {
            // public native void putOrderedLong(Object obj, long offset, long newValue);
            unsafeClass.getDeclaredMethod("putOrderedLong", Object.class, long.class, long.class).invoke(unsafe, obj, offset, newValue);
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
        try {
            // public native Object getObjectVolatile(Object obj, long offset);
            return unsafeClass.getDeclaredMethod("getObjectVolatile", Object.class, long.class).invoke(unsafe, obj, offset);
        } catch (Throwable e) {
            e(e);
            try {
                // public native Object getObject(Object obj, long offset);
                return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, obj, offset);
            } catch (Throwable e1) {
                e(e1);
            }
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
        try {
            //public native void putObjectVolatile(Object obj, long offset,  Object newValue);
            unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class).invoke(unsafe, obj, offset, newValue);
        } catch (Throwable e) {
            e(e);
            try {
                // public native void putObject(Object obj, long offset, Object newValue);
                unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, obj, offset, newValue);
            } catch (Throwable e1) {
                e(e1);
            }
        }
    }

    /******************************工具方案*****************************************************/


    //normalize是一种将signed int 转换为unsigned long 的方法，以便正确使用地址。
    public static long normalize(int value) {
        if (value >= 0) return value;
        return (~0L >>> 32) & value;
    }

    /**
     *
     * 自己实现的sizeOf, 算法：遍历所有非静态字段，包​​括所有超类，获取每个字段的偏移量，找到最大值并添加填充。
     * 另一个版本
     * public static long sizeOf(Object object){
     *     return getUnsafe().getAddress(
     *         normalize(getUnsafe().getInt(object, 4L)) + 12L);
     * }
     * 建议：为了获得良好、安全和准确的sizeof功能，
     *      最好使用 java.lang.instrument包，但它需要agent在你的 JVM 中指定ng选项。
     * @param o
     * @return
     */
    public static long sizeOf(Object o) {
        HashSet<Field> fields = new HashSet<Field>();
        Class c = o.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if ((f.getModifiers() & Modifier.STATIC) == 0) {
                    fields.add(f);
                }
            }
            c = c.getSuperclass();
        }

        // get offset
        long maxSize = 0;
        for (Field f : fields) {
            long offset = objectFieldOffset(f);
            if (offset > maxSize) {
                maxSize = offset;
            }
        }

        return ((maxSize / 8) + 1) * 8;   // padding
    }


    public static long getObjectAddress(Object obj) {
        try {
            Object[] array = new Object[]{obj};
            if (arrayIndexScale(Object[].class) == 8) {
                return getLong(array, arrayBaseOffset(Object[].class));
            } else {
                return 0xffffffffL & getInt(array, arrayBaseOffset(Object[].class));
            }
        } catch (Throwable e) {
            e(e);
            return -1;
        }
    }
//    public static long location(Object object){
//        Object[] array = new Object[] {object};
//        long baseOffset = arrayBaseOffset(Object[].class);
//        int addressSize = addressSize();
//        long location;
//        switch (addressSize) {
//            case 4:
//                location = getInt(array, baseOffset);
//                break;
//            case 8:
//                location = getLong(array, baseOffset);
//                break;
//            default:
//                return 0;
//        }
//        return (location);
//    }
    /**
     * get Object from address, refer: http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/
     * @param address the address of a object.
     * @return
     */
    public static Object getObject(long address) {
        Object[] array = new Object[]{null};
        long baseOffset = arrayBaseOffset(Object[].class);
        Logger.d("getObject() is64Bit: "+is64Bit());
        if (is64Bit()) {
            putLong(array, baseOffset, address);
        } else {
            putInt(array, baseOffset, (int) address);
        }
        return array[0];
    }

    private static boolean is64Bit() {
        try {
          return  (boolean) Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("is64Bit").invoke(Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("getRuntime").invoke(null));
        } catch (Throwable e) {
           e(e);
        }
        return false;

    }
/************************会崩库crash******************/
    /**
     * 获取对象的内存地址
     * @param obj
     * @return
     */
    public static long toAddress(Object obj) {
        Object[] array = new Object[]{obj};
//        long baseOffset = arrayBaseOffset(Object[].class);
//        return normalize(getInt(array, baseOffset));

        Logger.d("toAddress() arrayIndexScale: "+arrayIndexScale(Object[].class));
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
        // 原来偏移值未考虑32/64区别
//        putLong(array, baseOffset, address);
//        return array[0];
        Logger.d("fromAddress() arrayIndexScale: "+arrayIndexScale(Object[].class));
        if (arrayIndexScale(Object[].class) == 8) {
            putLong(array, baseOffset, address);
        }else{
            putInt(array, baseOffset, (int) address);
        }
        return array[0];
    }


//    /**
//     *浅拷贝 ,Java 可用 android 不可以使用
//     * @param obj
//     * @return
//     */
//  public  static Object shallowCopy(Object obj) {
//        long size = sizeOf(obj);
//        long start = toAddress(obj);
//        long address = getUnsafe().allocateMemory(size);
//        getUnsafe().copyMemory(start, address, size);
//        return fromAddress(address);
//    }

    private static void e(Throwable e) {
        Logger.e("UnsafeHelper", Log.getStackTraceString(e));
    }

}
