package me.weishu.epic.samples.tests.custom;

import java.lang.reflect.Method;

import me.weishu.epic.art.EpicNative;
import me.weishu.epic.art.method.ArtMethod;
import utils.Debug;
import utils.Logger;
import utils.Unsafe;
import utils.tt.ArtHelper;
import utils.tt.Refunsafe;
import utils.tt.UnsafeHelper;

/**
 * Created by weishu on 17/11/6.
 */

public class Case6 implements Case {

    private static final String TAG = "Case6";

    @Override
    public void hook() {
        Object test = new Object();
        Logger.i(TAG, "原始对象object:" + test);

        long testAddr = Unsafe.getObjectAddress(test);
        Logger.i(TAG, "Unsafe获取[getObjectAddress]地址:" + testAddr + "---" + Debug.longHex(testAddr));
//        Object ot1=Unsafe.getObject(testAddr);
//        Logger.i(TAG, "Unsafe [getObject]地址获取对象:" + ot1);


        Logger.i(TAG, "UnsafeHelper【toAddress】获取object地址:" + UnsafeHelper.toAddress(test) + "---" + Debug.longHex(testAddr));
        Logger.i(TAG, "UnsafeHelper[fromAddress] 获取对象:" + UnsafeHelper.fromAddress(testAddr));

        Logger.i(TAG, "EpicNative isUseUnsafe :" + EpicNative.isUseUnsafe());
        Logger.i(TAG, "EpicNative 获取对象:" + EpicNative.getObject(testAddr));


        /*********************************/
        Method m = Refunsafe.getMethod(Case6.class, "sayHello", String.class);
        ArtMethod artme = ArtMethod.of(m);
        Logger.i("方法比较："
                +"\r\ngetAddress:"+artme.getAddress()
                +"\r\ngetFieldOffset:"+artme.getFieldOffset()
                +"\r\ngetEntryPointFromQuickCompiledCode:"+artme.getEntryPointFromQuickCompiledCode()
                +"\r\ngetArtMethodSize:"+ArtMethod.getArtMethodSize()
                + "\r\n[ArtHelper]getMethodAddress:"+ ArtHelper.getMethodAddress(m)
                + "\r\n[ArtHelper]sizeOfArtMethod:"+ ArtHelper.sizeOfArtMethod()
//                + "\r\n[ArtHelper]getMethodIndexOffset:"+ ArtHelper.getMethodIndexOffset()
        );

//        final Method nanoTime = XposedHelpers.findMethodExact(System.class, "nanoTime");
//        final Method uptimeMillis = XposedHelpers.findMethodExact(SystemClock.class, "uptimeMillis");
//        final Method map = XposedHelpers.findMethodExact(Target.class, "test1", Object.class, int.class);
//        final Method malloc = XposedHelpers.findMethodExact(Target.class, "test3", Object.class, int.class);
//
//        ArtMethod artMethod1 = ArtMethod.of(nanoTime);
//        ArtMethod artMethod2 = ArtMethod.of(uptimeMillis);
//
//        ArtMethod artMethod3 = ArtMethod.of(map);
//        ArtMethod artMethod4 = ArtMethod.of(malloc);
//
//        Logger.i(TAG, "nanoTime: addr: 0x" + artMethod1.getAddress() + ", entry:" + Debug.addrHex(artMethod1.getEntryPointFromQuickCompiledCode()));
//        Logger.i(TAG, "uptimeMills: addr: 0x" + artMethod2.getAddress() + ", entry:" + Debug.addrHex(artMethod2.getEntryPointFromQuickCompiledCode()));
//        Logger.i(TAG, "map : addr: 0x" + artMethod3.getAddress() + ", entry:" + Debug.addrHex(artMethod3.getEntryPointFromQuickCompiledCode()));
//        Logger.i(TAG, "malloc: addr: 0x" + artMethod4.getAddress() + ", entry:" + Debug.addrHex(artMethod4.getEntryPointFromQuickCompiledCode()));
    }

    @Override
    public boolean validate(Object... args) {
        return true;
    }

    public static void sayHello(String he) {
        Logger.i(TAG, "sayHello.  hi, " + he);
    }

    public static void run(String he) {
        Logger.i(TAG, "run. " + he + " 拍起来快的像条狗！");
    }

}
