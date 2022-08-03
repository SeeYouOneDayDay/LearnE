package uts;

import android.util.Log;


/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: TODO
 * @Version: 1.0
 * @Create: 2017/11/10 17:24:57
 * @author: weishu /sanbo
 */
public class XL {

    private static final boolean DEBUG = true;

    public static final String preFix = "hiddenapistubs";

    public static void i(String msg) {
        println(Log.INFO, preFix, msg);
    }

    public static void i(String tag, String msg) {
        println(Log.INFO, preFix + "." + tag, msg);
    }

    public static void d(String msg) {
        println(Log.DEBUG, preFix, msg);
    }

    public static void d(String tag, String msg) {
        println(Log.DEBUG, preFix + "." + tag, msg);
    }

    public static void v(Throwable e) {
        println(Log.VERBOSE, preFix, Log.getStackTraceString(e));
    }

    public static void v(String msg) {
        println(Log.VERBOSE, preFix, msg);
    }

    public static void v(String tag, String msg) {
        println(Log.VERBOSE, preFix + "." + tag, msg);
    }

    public static void w(String msg) {
        println(Log.WARN, preFix, msg);
    }

    public static void w(String tag, String msg) {
        println(Log.WARN, preFix + "." + tag, msg);
    }

    public static void w(String tag, String msg, Throwable e) {
        println(Log.WARN, preFix + "." + tag, msg + "\n" + Log.getStackTraceString(e));
    }

    public static void e(String msg) {
        println(Log.ERROR, preFix, msg);
    }

    public static void e(Throwable e) {
        println(Log.ERROR, preFix, Log.getStackTraceString(e));
    }

    public static void e(String tag, String msg) {
        println(Log.ERROR, preFix + "." + tag, msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        println(Log.ERROR, preFix + "." + tag, msg + "\n" + Log.getStackTraceString(e));
    }

    public static int println(int priority, String tag, String msg) {
        if (!DEBUG) {
            return 0;
        }
        return Log.println(priority, tag, msg);
    }
}
