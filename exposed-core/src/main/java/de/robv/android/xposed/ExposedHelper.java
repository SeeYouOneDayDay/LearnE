package de.robv.android.xposed;

import java.lang.reflect.Member;

import utils.XL;

/**
 * Created by weishu on 17/11/30.
 */
public class ExposedHelper {

    private static final String TAG = "ExposedHelper";

    public static void initSeLinux(String processName) {
        SELinuxHelper.initOnce();
        SELinuxHelper.initForProcess(processName);
    }

    public static boolean isIXposedMod(Class<?> moduleClass) {
        XL.d(TAG, "module's classLoader : " + moduleClass.getClassLoader() + ", super: " + moduleClass.getSuperclass());
        XL.d(TAG, "IXposedMod's classLoader : " + IXposedMod.class.getClassLoader());

        return IXposedMod.class.isAssignableFrom(moduleClass);
    }


    public static XC_MethodHook.Unhook newUnHook(XC_MethodHook methodHook, Member member) {
        // @TODO by sanbo. 这是什么写法   实例化内部类
        return methodHook.new Unhook(member);
    }

    public static void callInitZygote(String modulePath, Object moduleInstance) throws Throwable {
        IXposedHookZygoteInit.StartupParam param = new IXposedHookZygoteInit.StartupParam();
        param.modulePath = modulePath;
        param.startsSystemServer = false;
        ((IXposedHookZygoteInit) moduleInstance).initZygote(param);
    }

    public static void beforeHookedMethod(XC_MethodHook methodHook, XC_MethodHook.MethodHookParam param) throws Throwable {
        methodHook.beforeHookedMethod(param);
    }

    public static void afterHookedMethod(XC_MethodHook methodHook, XC_MethodHook.MethodHookParam param) throws Throwable {
        methodHook.afterHookedMethod(param);
    }
}
