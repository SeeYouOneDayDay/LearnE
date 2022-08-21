///*
// * Copyright 2014-2015 Marvin Wißfeld
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package org.lsposed.hiddenapibypass;
//
//import android.util.Log;
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//
////http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/
////http://aospxref.com/android-7.1.2_r39/xref/libcore/ojluni/src/main/java/sun/misc/Unsafe.java
////http://aospxref.com/android-8.1.0_r81/xref/libcore/ojluni/src/main/java/sun/misc/Unsafe.java
////http://aospxref.com/android-9.0.0_r61/xref/libcore/ojluni/src/main/java/sun/misc/Unsafe.java
////http://aospxref.com/android-10.0.0_r47/xref/libcore/ojluni/src/main/java/sun/misc/Unsafe.java
////http://aospxref.com/android-11.0.0_r21/xref/libcore/ojluni/src/main/java/sun/misc/Unsafe.java
////http://aospxref.com/android-12.0.0_r3/xref/libcore/ojluni/src/main/java/sun/misc/Unsafe.java
////
//public final class Mu {
//
//    private static Object unsafe;
//    private static Class unsafeClass;
//
//    private Mu() {
//    }
//
//    static {
//        try {
//            unsafeClass = Class.forName("sun.misc.Unsafe");
//            //private static final com.swift.sandhook.utils.Unsafe theUnsafe = THE_ONE;
//            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
//            theUnsafe.setAccessible(true);
//            unsafe = theUnsafe.get(null);
//        } catch (Throwable e) {
//            LL.e("Field [theUnsafe] get com.swift.sandhook.utils.Unsafe Failed! \r\n" + Log.getStackTraceString(e));
//            try {
//                //private static final com.swift.sandhook.utils.Unsafe THE_ONE = new com.swift.sandhook.utils.Unsafe();
//                final Field theUnsafe = unsafeClass.getDeclaredField("THE_ONE");
//                theUnsafe.setAccessible(true);
//                unsafe = theUnsafe.get(null);
//            } catch (Throwable e2) {
//                LL.e("Field [THE_ONE] get com.swift.sandhook.utils.Unsafe Failed! \r\n" + Log.getStackTraceString(e));
//                try {
//                    //public static com.swift.sandhook.utils.Unsafe getUnsafe()
//                    final Method getUnsafe = unsafeClass.getDeclaredMethod("getUnsafe");
//                    getUnsafe.setAccessible(true);
//                    unsafe = getUnsafe.invoke(null);
//                } catch (Throwable e3) {
//                    LL.e("Method [getUnsafe] get com.swift.sandhook.utils.Unsafe Failed! \r\n" + Log.getStackTraceString(e3));
//                }
//            }
//        }
//    }
//
//
//    public static long objectFieldOffset(Field field) {
//        if (unsafe == null || unsafeClass == null) {
//            return 0;
//        }
//        try {
//            // android 4有接口:  private static native long objectFieldOffset0(Field field);
//            //所有版本均有接口：public long objectFieldOffset(Field field)
//            return (long) unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class).invoke(unsafe, field);
//        } catch (Throwable e) {
//            LL.e(e);
//        }
//        return 0;
//    }
//
//
//    public static long getLong(Object array, long offset) {
//        if (unsafe == null || unsafeClass == null) {
//            return 0;
//        }
//        try {
//            // public native long getLongVolatile(Object obj, long offset);
//            return (long) unsafeClass.getDeclaredMethod("getLongVolatile", Object.class, long.class).invoke(unsafe, array, offset);
//        } catch (Throwable e) {
//            LL.e(e);
//            try {
//                //public native long getLong(Object obj, long offset);
//                return (long) unsafeClass.getDeclaredMethod("getLong", Object.class, long.class).invoke(unsafe, array, offset);
//            } catch (Throwable e1) {
//                LL.e(e1);
//            }
//        }
//        return 0;
//    }
//
//
//    public static void putLong(Object array, long offset, long value) {
//        if (unsafe == null || unsafeClass == null) {
//            return;
//        }
//        try {
//            // public native void putLongVolatile(Object obj, long offset, long newValue);
//            unsafeClass.getDeclaredMethod("putLongVolatile", Object.class, long.class, long.class).invoke(unsafe, array, offset, value);
//        } catch (Throwable e) {
//            LL.e(e);
//            try {
//                //public native void putLong(Object obj, long offset, long newValue);
//                unsafeClass.getDeclaredMethod("putLong", Object.class, long.class, long.class).invoke(unsafe, array, offset, value);
//            } catch (Throwable e1) {
//                LL.e(e1);
//            }
//        }
//    }
//
//    public static int getInt(long offset) {
//        if (unsafe == null || unsafeClass == null) {
//            return 0;
//        }
//        try {
////            Method[]  ms=   unsafeClass.getDeclaredMethods();
////            if (ms!=null&&ms.length>0){
////                for (Method m:ms) {
////                    LL.d(m.toGenericString());
////                }
////            }
//            //public native int getInt(long address);
//            return (int) unsafeClass.getDeclaredMethod("getInt", long.class).invoke(unsafe, offset);
//        } catch (Throwable e1) {
//            LL.e(e1);
//        }
//        return 0;
//    }
//
//
//    public static Object getObject(Object obj, long offset) {
//        if (unsafe == null || unsafeClass == null) {
//            return null;
//        }
//        try {
//            //   public native Object getObjectVolatile(Object obj, long offset);
//            return unsafeClass.getDeclaredMethod("getObjectVolatile", Object.class, long.class).invoke(unsafe, obj, offset);
//        } catch (Throwable e) {
//            LL.e(e);
//            try {
//                // public native Object getObject(Object obj, long offset)
//                return unsafeClass.getDeclaredMethod("getObject", Object.class, long.class).invoke(unsafe, obj, offset);
//            } catch (Throwable e1) {
//                LL.e(e1);
//            }
//        }
//        return null;
//    }
//
//
//    public static void putObject(Object obj, long offset, Object newValue) {
//        if (unsafe == null || unsafeClass == null) {
//            return;
//        }
//        try {
//            // public native void putObjectVolatile(Object obj, long offset, Object newValue);
//            unsafeClass.getDeclaredMethod("putLongVolatile", Object.class, long.class, Object.class).invoke(unsafe, obj, offset, newValue);
//        } catch (Throwable e) {
//            LL.e(e);
//            try {
//                // public native void putObject(Object obj, long offset, Object newValue);
//                unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, obj, offset, newValue);
//            } catch (Throwable e1) {
//                LL.e(e1);
//            }
//        }
//    }
//
//    private static boolean is64Bit() {
//        try {
//            return (boolean) Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("is64Bit").invoke(Class.forName("dalvik.system.VMRuntime").getDeclaredMethod("getRuntime").invoke(null));
//        } catch (Throwable e) {
//            LL.e(e);
//        }
//        return false;
//    }
//
//    public static int arrayBaseOffset(Class cls) {
//        try {
//            return (int) unsafeClass.getDeclaredMethod("arrayBaseOffset", Class.class).invoke(unsafe, cls);
//        } catch (Throwable e) {
//            LL.e(e);
//            return 0;
//        }
//    }
//
//    public static int getInt(Object array, long offset) {
//        try {
//            return (int) unsafeClass.getDeclaredMethod("getInt", Object.class, long.class).invoke(unsafe, array, offset);
//        } catch (Throwable e) {
//            LL.e(e);
//            return 0;
//        }
//    }
//
//    public static void putInt(Object array, long offset, int value) {
//        try {
//            unsafeClass.getDeclaredMethod("putIntVolatile", Object.class, long.class, int.class).invoke(unsafe, array, offset, value);
//        } catch (Throwable e) {
//            LL.e(e);
//            try {
//                unsafeClass.getDeclaredMethod("putIntVolatile", Object.class, long.class, int.class).invoke(unsafe, array, offset, value);
//            } catch (Throwable e1) {
//                LL.e(e1);
//            }
//        }
//    }
//
//    /**
//     * get Object from address, refer: http://mishadoff.com/blog/java-magic-part-4-sun-dot-misc-dot-unsafe/
//     * @param address the address of a object.
//     * @return
//     */
//    public static Object getObject(long address) {
//        Object[] array = new Object[]{null};
//        long baseOffset = arrayBaseOffset(Object[].class);
//        if (is64Bit()) {
//            putLong(array, baseOffset, address);
//        } else {
//            putInt(array, baseOffset, (int) address);
//        }
//        return array[0];
//    }
//
//    public static long getObjectAddress(Object obj) {
//        try {
//            Object[] array = new Object[]{obj};
//            if (arrayIndexScale(Object[].class) == 8) {
//                return getLong(array, arrayBaseOffset(Object[].class));
//            } else {
//                return 0xffffffffL & getInt(array, arrayBaseOffset(Object[].class));
//            }
//        } catch (Throwable e) {
//            LL.e(e);
//            return -1;
//        }
//    }
//
//    public static int arrayIndexScale(Class cls) {
//        try {
//            return (int) unsafeClass.getDeclaredMethod("arrayIndexScale", Class.class).invoke(unsafe, cls);
//        } catch (Throwable e) {
//            LL.e(e);
//            return 0;
//        }
//    }
//}
