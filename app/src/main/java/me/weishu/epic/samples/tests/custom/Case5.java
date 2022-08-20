package me.weishu.epic.samples.tests.custom;

import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import me.weishu.epic.art.EpicNative;
import me.weishu.epic.art.method.ArtMethod;
import me.weishu.epic.samples.MainApplication;
import utils.Logger;
import utils.Unsafe;

/**
 * Created by weishu on 17/11/6.
 */

public class Case5 implements Case {
    @Override
    public void hook() {
        Method m = XposedHelpers.findMethodExact(TextView.class, "setPadding", int.class, int.class, int.class, int.class);
        Logger.d("Case5", "hook 在绑定之前 m:" + m.toString() + "----->" + ArtMethod.of(m).getAddress());

        DexposedBridge.findAndHookMethod(TextView.class, "setPadding", int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (param.thisObject != null) {
                    Logger.i("Case5", "----this:" + Long.toHexString(Unsafe.getObjectAddress(param.thisObject)));
                }
                if (param.method != null) {
                    Logger.i("Case5", "----mehod:" + Long.toHexString(EpicNative.getMethodAddress((Method) param.method)));
                }
                if (param.args != null) {
                    for (Object arg : param.args) {
                        Logger.i("Case5", "---param:" + arg);
                        if (arg != null) {
                            Logger.i("Case5", "---<" + arg.getClass() + "> : 0x" +
                                    Long.toHexString(Unsafe.getObjectAddress(arg)) + ", value: " + arg);
                        } else {
                            Logger.i("Case5", "----param: null");
                        }
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Logger.i("Case5", "----after");
            }
        });

        Method m1 = XposedHelpers.findMethodExact(TextView.class, "setPadding", int.class, int.class, int.class, int.class);
        Logger.d("Case5", "hook 在绑定之后 m1:" + m.toString() + "----->" + ArtMethod.of(m1).getAddress());

        Method[] ms = TextView.class.getDeclaredMethods();
        for (Method _m : ms) {
            if (_m.getName().contains("setPadding"))
                Logger.i("Case5", getMethodAddress(_m) + "----->" + _m.toString());
        }
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
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {

        }
        return 0L;
    }


    private static Object getFieldValue(Class superclass, String artMethod, Method method) {
        Field f = getField(superclass, artMethod);
        if (f != null) {
            try {
                return f.get(method);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
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

            }
        }
        return null;
    }

    @Override
    public boolean validate(Object... args) {
//public void setPadding(int left, int top, int right, int bottom) {
        Method m = XposedHelpers.findMethodExact(TextView.class, "setPadding", int.class, int.class, int.class, int.class);
        Logger.d("Case5", "执行前 m:" + m.toString() + "----->" + ArtMethod.of(m).getAddress());
        TextView tv = new TextView(MainApplication.getAppContext());
        tv.setPadding(99, 99, 99, 99);


        Logger.d("Case5", "--->" + tv.getLeft());
        return true;
    }
}
