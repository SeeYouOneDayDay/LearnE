package me.weishu.epic.samples.tests.custom;

import android.util.Log;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import utils.Logger;

/**
 * Created by weishu on 18/1/11.
 */
public class Case19 implements Case {
    private static final String TAG = "Case19";

    @Override
    public void hook() {
//        Object os = Refunsafe.getFieldValue("libcore.io.Libcore", "rawOs");
//
//        // libcore/luni/src/main/java/libcore/io/IoBridge.java
//        //   public static @NonNull InetSocketAddress getLocalInetSocketAddress(@NonNull FileDescriptor fd) throws SocketException {
//
//        Logger.d(TAG, "os:" + os.toString());


        DexposedBridge.findAndHookMethod(staticA.class, "add", int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Logger.d(TAG,"beforeHookedMethod add:" +param.args[0]+"----"+param.args[1]);
                param.args[0]=100;
//                super.beforeHookedMethod(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
                param.setResult(-1);
            }
        });
    }

    @Override
    public boolean validate(Object... args) {
        Logger.d(TAG,"add:" + staticA.add(0,100));

        return true;
    }

    static class staticA {
        public static int add(int x, int y) {
            return x + y;
        }
    }
}
