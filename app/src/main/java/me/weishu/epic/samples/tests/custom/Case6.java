package me.weishu.epic.samples.tests.custom;

import android.os.SystemClock;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import me.weishu.epic.art.EpicNative;
import me.weishu.epic.art.method.ArtMethod;
import utils.Debug;
import utils.Logger;
import utils.Unsafe;

/**
 * Created by weishu on 17/11/6.
 */

public class Case6 implements Case {

    private static final String TAG = "Case6";

    @Override
    public void hook() {
        Object test = new Object();
        Logger.i(TAG, "test object:" + test);

        long testAddr = Unsafe.getObjectAddress(test);
        Logger.i(TAG, "test object address :" + testAddr);
        Logger.i(TAG, "test object :" + EpicNative.getObject(XposedHelpers.getLongField(Thread.currentThread(), "nativePeer"), testAddr));

        // uts.Logger.i(TAG, "object:" + EpicNative.getObject())
        final Method nanoTime = XposedHelpers.findMethodExact(System.class, "nanoTime");
        final Method uptimeMillis = XposedHelpers.findMethodExact(SystemClock.class, "uptimeMillis");
        final Method map = XposedHelpers.findMethodExact(Target.class, "test1", Object.class, int.class);
        final Method malloc = XposedHelpers.findMethodExact(Target.class, "test3", Object.class, int.class);

        ArtMethod artMethod1 = ArtMethod.of(nanoTime);
        ArtMethod artMethod2 = ArtMethod.of(uptimeMillis);

        ArtMethod artMethod3 = ArtMethod.of(map);
        ArtMethod artMethod4 = ArtMethod.of(malloc);

        Logger.i(TAG, "nanoTime: addr: 0x" + artMethod1.getAddress() + ", entry:" + Debug.addrHex(artMethod1.getEntryPointFromQuickCompiledCode()));
        Logger.i(TAG, "uptimeMills: addr: 0x" + artMethod2.getAddress() + ", entry:" + Debug.addrHex(artMethod2.getEntryPointFromQuickCompiledCode()));
        Logger.i(TAG, "map : addr: 0x" + artMethod3.getAddress() + ", entry:" + Debug.addrHex(artMethod3.getEntryPointFromQuickCompiledCode()));
        Logger.i(TAG, "malloc: addr: 0x" + artMethod4.getAddress() + ", entry:" + Debug.addrHex(artMethod4.getEntryPointFromQuickCompiledCode()));
    }

    @Override
    public boolean validate(Object... args) {
        return true;
    }
}
