package me.weishu.epic.samples.tests.custom;

import android.util.Log;

import java.io.FileDescriptor;
import java.util.Arrays;

import de.robv.android.xposed.DexposedBridge;
import de.robv.android.xposed.XC_MethodHook;
import utils.Logger;
import utils.gs.Refunsafe;

/**
 * Created by weishu on 18/1/11.
 */
public class Case19 implements Case {
    private static final String TAG = "Case19";
    @Override
    public void hook() {
        Object os = Refunsafe.getFieldValue("libcore.io.Libcore", "rawOs");

        // libcore/luni/src/main/java/libcore/io/IoBridge.java
        //   public static @NonNull InetSocketAddress getLocalInetSocketAddress(@NonNull FileDescriptor fd) throws SocketException {

        Logger.d(TAG,"os:"+os.toString());
    }

    @Override
    public boolean validate(Object... args) {


        return true;
    }
}
