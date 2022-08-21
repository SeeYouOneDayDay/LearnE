package uts;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * 28 x86 crash
 *
 */

/**
 * 最小剪辑版黑灰名单避免方案.支持JDK 7、8编译的环境.
 * 用法使用一个接口方案: MinRefUtils.unseal(Context context)
 */
public final class MinRefOneClass {
    private static final String TAG = "sanbo." + MinRefOneClass.class.getSimpleName();

    private transient int accessFlags; //
    private transient int classFlags; //
    private transient ClassLoader classLoader; //
    private transient int classSize; //
    private transient int clinitThreadId; //
    private transient Class<?> componentType; //
    private transient short copiedMethodsOffset; //
    private transient Object dexCache; //
    private transient int dexClassDefIndex; //
    private volatile transient int dexTypeIndex;//
    // 方法原型：private transient ClassExt extData;
    private transient Object extData; //
    private transient long iFields; //
    private transient Object[] ifTable; //
    private transient long methods; //
    private transient String name;//
    private transient int numReferenceInstanceFields; //
    private transient int numReferenceStaticFields; //
    private transient int objectSize; //
    private transient int objectSizeAllocFastPath; //
    private transient int primitiveType; //
    private transient int referenceInstanceOffsets; //
    private transient long sFields; //
    private transient int status; //
    private transient Class<?> superClass; //
    private transient short virtualMethodsOffset;
    private transient Object vtable;

    private static void fun1() {
    }

    private static void fun2() {
    }

    private static Object invoke(Object... args) {
        throw new IllegalStateException("Failed to invoke the method");
    }

    private MinRefOneClass(Object... args) {
        throw new IllegalStateException("Failed to new a instance");
    }

    // 对应 Executable.artMethod 等价于 JNI artmethod 指针
    private static long artMethodOffset;
    // 方法数对应，与JNI Class uint64_t methods_;
    private static long methodsOffset;
    // art方法的大小
    private static long artMethodSize;
    //art方法偏差--@todo check
    private static long artMethodBias;
    private static Object unsafeObj;
    private static Class unsafeClass;

    static {
        try {
            if (unsafeClass == null) {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            //private static final Unsafe theUnsafe = THE_ONE;
            unsafeObj = getFieldValue(unsafeClass, "theUnsafe", null);
            if (unsafeObj == null) {
                //private static final Unsafe THE_ONE = new Unsafe();
                unsafeObj = getFieldValue(unsafeClass, "THE_ONE", null);
            }
            if (unsafeObj == null) {
                unsafeObj = call(unsafeClass, "getUnsafe", null, null, null);
            }
        } catch (Throwable e) {
            e(e);
        }
        try {
            artMethodOffset = (long) call(unsafeClass, "objectFieldOffset", unsafeObj
                    , new Class[]{Field.class}, new Object[]{getField(Method.class.getSuperclass(), "artMethod")});
//            methodOffset = Unsafe.objectFieldOffset(Method.class.getSuperclass().getDeclaredField("artMethod"));
//            methodsOffset = Unsafe.objectFieldOffset(MinRef.class.getDeclaredField("methods"));
            methodsOffset = (long) call(unsafeClass, "objectFieldOffset", unsafeObj
                    , new Class[]{Field.class}, new Object[]{getField(MinRefOneClass.class, "methods")});
            Method mA = MinRefOneClass.class.getDeclaredMethod("fun1");
            Method mB = MinRefOneClass.class.getDeclaredMethod("fun2");
            mA.setAccessible(true);
            mB.setAccessible(true);
            long aAddr = getMethodAddress(mA);
            long bAddr = getMethodAddress(mB);
//            long aMethods = Unsafe.getLong(MinRef.class, methodsOffset);
            long aMethods = (long) call(unsafeClass, "getLong", unsafeObj
                    , new Class[]{Object.class, long.class}, new Object[]{MinRefOneClass.class, methodsOffset});
            artMethodSize = bAddr - aAddr;
            artMethodBias = aAddr - aMethods - artMethodSize;

            i("获取所有参数结束,详情如下: "
                    + "\r\n    methodOffset (Executable artMethod):              " + artMethodOffset
                    + "\r\n    methodsOffset (Class  long methods):              " + methodsOffset
                    + "\r\n    artMethodSize (addressA - addressB):              " + artMethodSize
                    + "\r\n    artMethodBias (addressA-aMethods-artMethodSize):  " + artMethodBias
            );
            if (android.os.Build.VERSION.SDK_INT > 27) {
                exemptAll();
            }
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    static boolean checkArgsForInvokeMethod(Class[] params, Object[] args) {
        if (params.length != args.length) return false;
        for (int i = 0; i < params.length; ++i) {
            if (params[i].isPrimitive()) {
                if (params[i] == int.class && !(args[i] instanceof Integer)) return false;
                else if (params[i] == byte.class && !(args[i] instanceof Byte)) return false;
                else if (params[i] == char.class && !(args[i] instanceof Character)) return false;
                else if (params[i] == boolean.class && !(args[i] instanceof Boolean)) return false;
                else if (params[i] == double.class && !(args[i] instanceof Double)) return false;
                else if (params[i] == float.class && !(args[i] instanceof Float)) return false;
                else if (params[i] == long.class && !(args[i] instanceof Long)) return false;
                else if (params[i] == short.class && !(args[i] instanceof Short)) return false;
            } else if (args[i] != null && !params[i].isInstance(args[i])) return false;
        }
        return true;
    }


    /**
     * invoke a restrict method named {@code methodName} of the given class {@code clazz} with this object {@code thiz} and arguments {@code args}
     *
     * @param clazz      the class call the method on (this parameter is required because this method cannot call inherit method)
     * @param thiz       this object, which can be {@code null} if the target method is static
     * @param methodName the method name
     * @param args       arguments to call the method with name {@code methodName}
     * @return the return value of the method
     * @see Method#invoke(Object, Object...)
     */
    public static Object invoke(Class<?> clazz, Object thiz, String methodName, Object... args) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (thiz != null && !clazz.isInstance(thiz)) {
            throw new IllegalArgumentException("this object is not an instance of the given class");
        }
        //实际上如果只是为了简单调用这个函数，完全可以直接随便找一个 Method 并用 Unsafe 去设置它的 artMethod，然后直接调用 Method.invoke 即可。
        // 因为从 Method.invoke 的 native 代码可以看到，调用过程仅使用 Method 的 artMethod 成员。
        Method stub = MinRefOneClass.class.getDeclaredMethod("invoke", Object[].class);
        stub.setAccessible(true);
//        long methods = Unsafe.getLong(clazz, methodsOffset);
        long methods = (long) call(unsafeClass, "getLong", unsafeObj
                , new Class[]{Object.class, long.class}, new Object[]{clazz, methodsOffset});
        if (methods == 0) {
            throw new NoSuchMethodException("(invoke)Cannot find matching method");
        }
//        int numMethods = Unsafe.getInt(methods);
        int numMethods = (int) call(unsafeClass, "getInt", unsafeObj
                , new Class[]{long.class}, new Object[]{methods});
        d("[invoke] " + clazz.getName() + " " + methodName + "()  has " + numMethods + " methods");
        for (int i = 0; i < numMethods; i++) {
            //第I个元素  base + i * size  + 偏差值？
            long method = methods + i * artMethodSize + artMethodBias;
//            Unsafe.putLong(stub, methodOffset, method);
            call(unsafeClass, "putLong", unsafeObj
                    , new Class[]{Object.class, long.class, long.class}, new Object[]{stub, artMethodOffset, method});
//            //////////Just for long begin////////////
//            StringBuffer sb = new StringBuffer();
//            sb.append("[invoke获取方法]  ").append(clazz.getName()).append(".").append(stub.getName());
//            Class<?>[] parameterTypes = stub.getParameterTypes();
//            if (parameterTypes != null && parameterTypes.length > 0) {
//                sb.append(Arrays.asList(parameterTypes).toString());
//            }
//            v(sb.toString());
//            //////////Just for long end////////////
            if (methodName.equals(stub.getName())) {
                Class<?>[] params = stub.getParameterTypes();
                if (checkArgsForInvokeMethod(params, args))
                    return stub.invoke(thiz, args);
            }
        }
        throw new NoSuchMethodException("invoke Cannot find matching method");
    }

    /**
     * Sets the list of exemptions from hidden API access enforcement.
     *
     * @param signaturePrefixes A list of class signature prefixes. Each item in the list is a prefix match on the type
     *                          signature of a blacklisted API. All matching APIs are treated as if they were on
     *                          the whitelist: access permitted, and no logging..
     * @return whether the operation is successful
     */
    public static boolean setHiddenApiExemptions(String... signaturePrefixes) {
        try {
            Object runtime = invoke(Class.forName("dalvik.system.VMRuntime"), null, "getRuntime");
            invoke(Class.forName("dalvik.system.VMRuntime"), runtime, "setHiddenApiExemptions", (Object) signaturePrefixes);
            return true;
        } catch (Throwable e) {
            e("setHiddenApiExemptions\r\n" + Log.getStackTraceString(e));
            return false;
        }
    }

    public static int unseal(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 28) {
            // Below Android P, ignore
            return 0;
        }

        // try exempt API first.
        if (exemptAll()) {
            return 0;
        }
        return -1;
    }

    static boolean isInit = false;

    static boolean exemptAll() {
        if (!isInit) {
            isInit = setHiddenApiExemptions(new String[]{
                    "L"
//                , "Landroid/" "Lcom/", "Ljava/", "Ldalvik/", "Llibcore/", "Lsun/", "Lhuawei/"
            });
        }
        return isInit;
    }



    public static long getMethodAddress(Method method) {
        try {
            if (method == null) {
                return 0L;
            }
            Object mirrorMethod = getFieldValue(Method.class.getSuperclass(), "artMethod", method);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
            return toAddress(mirrorMethod);
        } catch (Throwable e) {
            e(e);
        }
        return 0L;
    }

    public static long toAddress(Object obj) {
        Object[] array = new Object[]{obj};
        int arrayIndexScaleObject = (int) call(unsafeClass, "arrayIndexScale", null, new Class[]{Class.class}, new Object[]{Object[].class});
        int arrayBaseOffsetObject = (int) call(unsafeClass, "arrayBaseOffset", null
                , new Class[]{Class.class}, new Object[]{Object[].class});
        if (arrayIndexScaleObject == 8) {
            return (long) call(unsafeClass, "getLong", null
                    , new Class[]{Object.class, long.class}, new Object[]{array, arrayBaseOffsetObject});
        } else {
            int x = (int) call(unsafeClass, "getInt", null
                    , new Class[]{Object.class, long.class}, new Object[]{array, arrayBaseOffsetObject});
            return 0xffffffffL & x;
        }
    }

    public static Object getFieldValue(Class<?> clazz, String fieldName, Object instance) {
        try {
            Field addr = getField(clazz, fieldName);
            if (addr != null) {
                return addr.get(instance);
            }
        } catch (Throwable e) {
            e(e);
        }
        return null;
    }

    public static Field getField(Class<?> clazz, String fieldName) {
        try {
            Field addr = clazz.getDeclaredField(fieldName);
            if (addr != null) {
                addr.setAccessible(true);
                return addr;
            }
        } catch (Throwable e) {
            try {
                Field addr = clazz.getField(fieldName);
                if (addr != null) {
                    addr.setAccessible(true);
                    return addr;
                }
            } catch (Throwable ex) {
                e(ex);
            }
        }
        return null;
    }

    public static Object call(Class<?> clazz, String methodName, Object receiver, Class[] types, Object[] params) {
        if (clazz == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        try {
            if (types == null || params == null) {
                Method method = getMethod(clazz, methodName);
                if (method != null) {
                    return method.invoke(receiver);
                }
            } else {
                Method method = getMethod(clazz, methodName, types);
                if (method != null) {
                    return method.invoke(receiver, params);
                }
            }
        } catch (Throwable throwable) {
            e(throwable);
        }
        return null;
    }

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... types) {
        if (clazz == null || TextUtils.isEmpty(methodName)) {
            return null;
        }
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, types);
        } catch (Throwable e) {
        }
        if (method == null) {
            try {
                method = clazz.getMethod(methodName, types);
            } catch (Throwable e) {
            }
        }
        if (method != null) {
            method.setAccessible(true);
        }
        return method;
    }

    static void v(String s) {
        Log.println(Log.VERBOSE, TAG, s);
    }

    static void d(String s) {
        Log.println(Log.DEBUG, TAG, s);
    }

    static void i(String s) {
        Log.println(Log.INFO, TAG, s);
    }

    static void e(String s) {
        Log.println(Log.ERROR, TAG, s);
    }

    static void e(Throwable e) {
        Log.println(Log.ERROR, TAG, Log.getStackTraceString(e));
    }

}
