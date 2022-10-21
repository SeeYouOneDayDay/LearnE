package utils;

import android.text.TextUtils;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: 父类子类判断
 * @Version: 1.0
 * @Create: 2022-08-03 18:14:50
 * @author: sanbo
 */
public class Subclass {

    public static boolean isSubClass(Object subObject, String fatherClass) {
        if (TextUtils.isEmpty(fatherClass) || subObject == null) {
            return false;
        }
        return isSubClass(subObject.getClass(), Reflect.findClass(fatherClass));
    }

    public static boolean isSubClass(Object subObject, Class fatherClass) {
        if (fatherClass == null || subObject == null) {
            return false;
        }
        return isSubClass(subObject.getClass(), fatherClass);
    }

    public static boolean isSubClass(Class<?> subClass, String fatherClass) {
        if (TextUtils.isEmpty(fatherClass) || subClass == null) {
            return false;
        }
        return isSubClass(subClass, Reflect.findClass(fatherClass));
    }

    /**
     * 判断是否两个类是否是有祖、父类关系
     *
     * @param subClass
     * @param fatherClass
     * @return
     */
    public static boolean isSubClass(Class<?> subClass, Class<?> fatherClass) {
        if (subClass == null || fatherClass == null) {
            return false;
        }
        Class<?> tempClass = subClass;
        while (!tempClass.equals(Object.class)) {
            if (tempClass == fatherClass) {
                return true;
            }
            tempClass = tempClass.getSuperclass();
        }
        return false;
    }


}
