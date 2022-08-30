package me.weishu.epic.samples.tests.custom;

import android.widget.TextView;

import java.lang.reflect.Method;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import me.weishu.epic.art.EpicNative;
import me.weishu.epic.samples.MainApplication;
import utils.Logger;
import utils.gs.UnsafeHelper;

/**
 * Created by weishu on 17/11/6.
 */
public class Case5 implements Case {
    private Method setPaddingInTextView = null;

    @Override
    public void hook() {
        setPaddingInTextView = XposedHelpers.findMethodExact(TextView.class, "setPadding", int.class, int.class, int.class, int.class);
        Logger.d("Case5", "hook 在绑定之前 :" + setPaddingInTextView.toString()
                + "----->" +EpicNative.getMethodAddress(setPaddingInTextView));

        DexposedBridge.findAndHookMethod(TextView.class, "setPadding", int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                if (param.thisObject != null) {
                    Logger.i("Case5", "----this:" + Long.toHexString(UnsafeHelper.toAddress(param.thisObject)));
                }
                if (param.method != null) {
                    Logger.i("Case5", "----mehod:" + Long.toHexString(EpicNative.getMethodAddress((Method) param.method)));
                }
                if (param.args != null) {
                    for (Object arg : param.args) {
                        Logger.i("Case5", "---param:" + arg);
                        if (arg != null) {
                            Logger.i("Case5", "---<" + arg.getClass() + "> : 0x" +
                                    Long.toHexString(UnsafeHelper.toAddress(arg)) + ", value: " + arg);
                        } else {
                            Logger.i("Case5", "----param: null");
                        }
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Logger.d("Case5", "--afterHookedMethod----:" + setPaddingInTextView.toString()
                        + "----->" + EpicNative.getMethodAddress(setPaddingInTextView));
            }
        });

//        Method[] ms = TextView.class.getDeclaredMethods();
//        for (Method _m : ms) {
//            if (_m.getName().contains("setPadding"))
//                Logger.i("Case5", "hook 在绑定之后 m1:" + ArtHelper.getMethodAddress(_m) + "----->" + _m.toString());
//        }
    }


    @Override
    public boolean validate(Object... args) {
        Logger.d("Case5", "---validate--:" + setPaddingInTextView.toString() + "----->"
                + EpicNative.getMethodAddress(setPaddingInTextView));
        TextView tv = new TextView(MainApplication.getAppContext());
        tv.setPadding(99, 99, 99, 99);


        Logger.d("Case5", "执行后--->" + tv.getLeft());
        return true;
    }
}
