package me.weishu.epic.samples.tests.fsx;

import java.lang.reflect.Method;

import utils.Logger;

public class ArtHelper {

    //原理： 同一个类中ArtMethod在内存地址是按顺序紧密排列的
    public static long sizeOfArtMethod() {
        Method method1 = MemoryHelper.getMethod(NeverUse.class, "method1");
        Method method2 = MemoryHelper.getMethod(NeverUse.class, "method2");
        long method1Address = MemoryHelper.getMethodAddress(method1);
        long method2Address = MemoryHelper.getMethodAddress(method2);
        return Math.abs(method2Address - method1Address);
    }
    public static Method getM1(){
        try {
            return NeverUse.class.getDeclaredMethod("method1");
        } catch (Throwable e) {
            Logger.e(e);
        }
        return null;
    }
    public static Method getM2(){
        try {
            return NeverUse.class.getDeclaredMethod("method2");
        } catch (Throwable e) {
            Logger.e(e);
        }
        return null;
    }
    public static class NeverUse {
        public static void method1() {
        }

        public static void method2() {
        }
    }
}
