package me.weishu.epic.samples.tests.custom;

import java.lang.reflect.Method;

import me.weishu.epic.art.method.ArtMethod;
import me.weishu.epic.samples.tests.fsx.ArtHelper;
import me.weishu.epic.samples.tests.fsx.MemoryHelper;
import utils.Logger;
import utils.UnsafeHelper;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: TODO
 * @Version: 1.0
 * @Create: 2022-08-10 17:17:20
 * @author: sanbo
 */
public class Case01 implements Case {

    private static final String TAG = "Case01";

    @Override
    public void hook() {
//        Object test = new Object();

//        Logger.i("===============方法地址及偏移测试=========================");
//        ArtMethod am = ArtMethod.of(ArtHelper.getM1());
//        ArtMethod am2 = ArtMethod.of(ArtHelper.getM2());
//        long ad1 = am.getAddress();
//        long ad2 = am2.getAddress();
//        Logger.i(TAG, "ArtMethod地址对比  artmethod1 address:" + ad1 + ", artmethod2 address:" + ad2 + "---->" + (ad2 - ad1));
//        Logger.i(TAG, "ArtMethod offset  ad1入口点的偏移量:" + am.getEntryPointFromQuickCompiledCode()
//                + ", ad2入口点的偏移量:" + am2.getEntryPointFromQuickCompiledCode()
//                + ", ad1访问标志的偏移量:" + am.getAccessFlags()
//                + ", ad2访问标志的偏移量:" + am2.getAccessFlags()
//                + ", ad Jni入口点的偏移量:" + am.getEntryPointFromJni()
//                + ", ad2 Jni入口点的偏移量:" + am2.getEntryPointFromJni()
//
//        );
//        Logger.i("===============地址对比case2=========================");
//
//        long m1 = MemoryHelper.getMethodAddress(ArtHelper.getM1());
//        long m2 = MemoryHelper.getMethodAddress(ArtHelper.getM2());
//        Logger.i(TAG, "toAddress  am:" + m1 + ", am2:" + m2 + "---->" + (m2 - m1));
//
//        Logger.d(TAG, "地址offset:" + (am2.getAddress() - am.getAddress())
//                + "----sizeOfArtMethod：" + ArtHelper.sizeOfArtMethod());
        Logger.i("==============地址对比完毕=========================");
        // 测试一个逻辑 来自JAndFix https://github.com/SeeYouOneDayDay/JAndFix.git

        Method method1=ArtHelper.getM1();
        // 取m1序列
        int method1MethodIndex = 0;
        Method[] methods = ArtHelper.NeverUse.class.getDeclaredMethods();
        for (int i = 0, size = methods.length; i < size; i++) {
            if (methods[i].equals(method1)) {
                //why +1? Becase "FindVirtualMethodForVirtualOrInterface(method, sizeof(void*))" has the offset of sizeof(void*)
                method1MethodIndex = i + 1;
                break;
            }
        }
        Logger.i(TAG,"method1MethodIndex: "+method1MethodIndex);
        int methodIndexOffset=0;
        int len= (int) (ArtHelper.sizeOfArtMethod() / 4);
        for (int i = 1; i < len; i++) {
            int value1 = UnsafeHelper.getInt(null, MemoryHelper.getMethodAddress(ArtHelper.getM1()) + i * 4);
            int value2 = UnsafeHelper.getInt(null, MemoryHelper.getMethodAddress(ArtHelper.getM2()) + i * 4);
            Logger.d(TAG,"【"+i+"】 value1: "+value1+"---"+method1MethodIndex+"   , value2: "+value2+"---"+(method1MethodIndex + 1));
//            Logger.d(TAG,"Plan B value1: "+UnsafeHelper.getInt(MemoryHelper.getMethodAddress(ArtHelper.getM1()) + i * 4)
//                    +" , value2: "+UnsafeHelper.getInt(MemoryHelper.getMethodAddress(ArtHelper.getM2()) + i * 4)
//            );
            if (value1 == method1MethodIndex
                    && value2 == method1MethodIndex + 1) {
                Logger.i(TAG,"["+i+"]一样了！！！value1:"+value1 +" ; value2: "+ value2);
                methodIndexOffset = i * 4;
            }
        }
        Logger.i(TAG,"methodIndexOffset:"+methodIndexOffset);


    }

    @Override
    public boolean validate(Object... args) {
        return true;
    }
}
