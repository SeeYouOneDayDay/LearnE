package uts;

/**
 * @Copyright © 2022 sanbo Inc. All rights reserved.
 * @Description: 父类子类判断
 * @Version: 1.0
 * @Create: 2022-08-03 18:14:50
 * @author: sanbo
 */
public class Subclass {
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
            if (tempClass.getName().equals(fatherClass.getName())) {
                return true;
            }
            tempClass = tempClass.getSuperclass();
        }
        return false;
    }

    public static boolean isSubClass(Class<?> subClass, String fatherClass) {
        if (subClass == null || fatherClass == null) {
            return false;
        }
        Class<?> tempClass = subClass;
        while (!tempClass.equals(Object.class)) {
            if (tempClass.getName().equals(fatherClass)) {
                return true;
            }
            tempClass = tempClass.getSuperclass();
        }
        return false;
    }
}
