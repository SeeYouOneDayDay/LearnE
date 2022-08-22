package utils.gs;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;
import me.weishu.epic.art.EpicNative;
import me.weishu.epic.art.method.ArtMethod;
import utils.Logger;

public class ArtHelper {
    public static long getConstructorAddress(Constructor constructor) {
        try {
            if (constructor == null) {
                return 0L;
            }
            Object mirrorMethod = Refunsafe.getFieldValue(Constructor.class.getSuperclass(), "artMethod", constructor);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
//            return UnsafeHelper.getObjectAddress(mirrorMethod);
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {
            Logger.e(e);
        }
        return 0L;
    }

    public static long getMethodAddress(Method method) {
        try {
            if (method == null) {
                return 0L;
            }
            Object mirrorMethod = Refunsafe.getFieldValue(Method.class.getSuperclass(), "artMethod", method);
            if (mirrorMethod.getClass().equals(Long.class)) {
                return (Long) mirrorMethod;
            }
            return UnsafeHelper.toAddress(mirrorMethod);
        } catch (Throwable e) {
            Logger.e(e);
        }
        return 0L;
    }

    public static long getFieldAddress(Field field) {
        try {
            if (field == null) {
                return 0L;
            }
            Method getArtField = Refunsafe.getMethod(Field.class, "getArtField");
            return (long) Refunsafe.invoke(field, getArtField);
        } catch (Throwable e) {
            Logger.e(e);
        }
        return 0L;
    }


    /**
     * The size of an art::mirror::ArtMethod, we use two rule method to measure the size
     * @return the size
     */
    public static int sizeOfArtMethod() {
        final Method rule1 = XposedHelpers.findMethodExact(ArtMethod.class, "rule1");
        final Method rule2 = XposedHelpers.findMethodExact(ArtMethod.class, "rule2");
        final long rule2Address = ArtHelper.getMethodAddress(rule2);
        final long rule1Address = ArtHelper.getMethodAddress(rule1);
        final long size = Math.abs(rule2Address - rule1Address);
        Logger.d("getArtMethodSize() art Method "
                + "\r\n\tsize: " + size + "--JJ--" + Math.abs(EpicNative.getMethodAddress(rule2) - EpicNative.getMethodAddress(rule1))
                + "\r\n\trule2Address: " + rule2Address + "--JJ--" + EpicNative.getMethodAddress(rule2)
                + "\r\n\trule1Address: " + rule1Address + "--JJ--" + EpicNative.getMethodAddress(rule1)
        );
        return (int) size;
    }

}
