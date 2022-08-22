package me.weishu.epic.samples.tests.custom;

import android.util.Log;

import utils.Debug;

import java.lang.reflect.Method;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import me.weishu.epic.art.method.ArtMethod;
import utils.Logger;

/**
 * Created by weishu on 18/1/8.
 */

public class Case16_SameEntry implements Case {
    private static final String TAG = "Case16_SameEntry";

    @Override
    public void hook() {
        final Method add = XposedHelpers.findMethodExact(Target.class, "add", int.class, int.class);
        final Method plus = XposedHelpers.findMethodExact(Target.class, "plus", int.class, int.class);

        ArtMethod artMethod3 = ArtMethod.of(add);
        ArtMethod artMethod4 = ArtMethod.of(plus);

        Logger.i(TAG, "plus: addr: " + Debug.addrHex(artMethod3.getAddress()) + ", entry:"
                + Debug.addrHex(artMethod3.getEntryPointFromQuickCompiledCode()));
        Logger.i(TAG, "milus: addr: " + Debug.addrHex(artMethod4.getAddress()) + ", entry:"
                + Debug.addrHex(artMethod4.getEntryPointFromQuickCompiledCode()));

        DexposedBridge.hookMethod(add, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Logger.d(TAG, "beforeHookedMethod() called with: param = [" + param + "]");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Logger.d(TAG, "afterHookedMethod() called with: param = [" + param + "]");
            }
        });

        DexposedBridge.hookMethod(plus, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Logger.d(TAG, "beforeHookedMethod() called with: param = [" + param + "]");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Logger.d(TAG, "afterHookedMethod() called with: param = [" + param + "]");
            }
        });
    }

    @Override
    public boolean validate(Object... args) {
        Target target = new Target();
        int add = target.add(1, 2);
        Logger.i(TAG, "1 + 2 = " + add);
        int plus = target.plus(3, 4);
        Logger.i(TAG, "3 + 4 = " + plus);
        return true;
    }
}
