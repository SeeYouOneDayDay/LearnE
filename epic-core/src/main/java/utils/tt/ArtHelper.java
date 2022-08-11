package utils.tt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import me.weishu.epic.art.EpicNative;
import utils.Logger;

public class ArtHelper {


    public static long getMethodAddress(Method method) {
        try {
            if (method == null) {
                return 0L;
            }
            Object mirrorMethod = Refunsafe.getField(Method.class.getSuperclass(), "artMethod", method);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
//            return UnsafeHelper.getObjectAddress(mirrorMethod);
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {
            e(e);
        }
        return 0L;
    }

    public static long getMethodAddress(Constructor constructor) {
        try {
            if (constructor == null) {
                return 0L;
            }
            Object mirrorMethod = Refunsafe.getField(Constructor.class.getSuperclass(), "artMethod", constructor);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
//            return UnsafeHelper.getObjectAddress(mirrorMethod);
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {
            e(e);
        }
        return 0L;
    }

    //原理： 同一个类中ArtMethod在内存地址是按顺序紧密排列的
    // https://github.com/SeeYouOneDayDay/JAndFix.git
    // https://github.com/WindySha/AndFixProject
    public static long sizeOfArtMethod() {
        Method method1 = Refunsafe.getMethod(NeverUse.class, "method1");
        Method method2 = Refunsafe.getMethod(NeverUse.class, "method2");
        long method1Address = getMethodAddress(method1);
        long method2Address = getMethodAddress(method2);
        return Math.abs(method2Address - method1Address);
    }


    // 考虑方式获取 Method.class变量遍历获取
    // Before Oreo, it is: java.lang.reflect.AbstractMethod
    // After Oreo, it is: java.lang.reflect.Executable
    // 貌似在安卓5.x有效.
    // 1. 获取Method （AbstractMethod）的变量methodIndex
    // 2. 通过Unsafe.objectFieldOffset(filed)获取对应值
    public static long getMethodIndexOffset() {
        Method method1 = Refunsafe.getMethod(NeverUse.class, "method1");
        // 取m1序列
        int method1MethodIndex = 0;
        Method[] methods = NeverUse.class.getDeclaredMethods();
        for (int i = 0, size = methods.length; i < size; i++) {
            if (methods[i].equals(method1)) {
                //why +1? Becase "FindVirtualMethodForVirtualOrInterface(method, sizeof(void*))" has the offset of sizeof(void*)
                method1MethodIndex = i + 1;
                break;
            }
        }
        int methodIndexOffset = 0;
        int len = (int) (ArtHelper.sizeOfArtMethod() / 4);
        for (int i = 1; i < len; i++) {
            Method m1 = Refunsafe.getMethod(NeverUse.class, "method1");
            Method m2 = Refunsafe.getMethod(NeverUse.class, "method2");
            long address1 = getMethodAddress(m1);
            long address2 = getMethodAddress(m2);
            long mm1 = EpicNative.getMethodAddress(m1);
            long mm2 = EpicNative.getMethodAddress(m2);
            i("地址对比:\r\n\t[" + i + "]"
                    + "\r\n\taddress1:" + address1 + " ; address2: " + address2
                    + "\r\n\t\tEpicNative ad1:" + mm1 + " ;ad2:" + mm2
            );

             UnsafeHelper.getLong(address1 + i * 4);

             i("-----------1------");
            int value1= UnsafeHelper.getInt(address1 + i * 4);
            int value2= UnsafeHelper.getInt(address1 + i * 4);

            i("----------2------");
            // @todo 检查崩溃
//            int value1 = UnsafeHelper.getInt(null, address1 + i * 4);
//            int value2 = UnsafeHelper.getInt(null, address2 + i * 4);
            d("【" + i + "】 value1: " + value1 + "---" + method1MethodIndex + "   , value2: " + value2 + "---" + (method1MethodIndex + 1));
            if (value1 == method1MethodIndex
                    && value2 == method1MethodIndex + 1) {
                e("[" + i + "]一样了！！！value1:" + value1 + " ; value2: " + value2);
                methodIndexOffset = i * 4;
            }

        }

        return methodIndexOffset;
    }




    /**
     * @Copyright © 2022 sanbo Inc. All rights reserved.
     * @Description: 用户变量偏差值获取
     * @Version: 1.0
     * @Create: 2022-08-11 11:19:21
     * @author: sanbo
     */
    public static class NeverUse {
        public static void method1() {
        }

        public static void method2() {
        }

        public static void method3() {
        }
    }


    /*****************************基础方法***********************/
    private static void d(String s) {
        Logger.d(s);
    }

    private static void i(String s) {
        Logger.i(s);
    }
    private static void e(String s) {
        Logger.e(s);
    }

    public static void e(Throwable e) {
        Logger.e(e);
    }

}
