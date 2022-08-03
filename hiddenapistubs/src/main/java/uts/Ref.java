package uts;


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Ref {


    public static Class<?> findClass(String className, ClassLoader classLoader) {
        if (classLoader == null)
            classLoader = ClassLoader.getSystemClassLoader();
        try {
            return ClassUtils.getClass(classLoader, className, false);
        } catch (ClassNotFoundException e) {
            XL.e(e);
        }
        return null;
    }


    public static Object getObjectField(Object obj, String fieldName) {
        try {
            Field field = findFieldRecursiveImpl(obj.getClass(), fieldName);
            field.setAccessible(true);
            if (field != null) {
                return field.get(obj);
            }
        } catch (Throwable e) {
            XL.e(e);
        }
        return null;
    }


    private static Field findFieldRecursiveImpl(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            while (true) {
                clazz = clazz.getSuperclass();
                if (clazz == null || clazz.equals(Object.class))
                    break;

                try {
                    return clazz.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        return null;
    }


    public static Field findFieldIfExists(Class<?> clazz, String fieldName) {
        try {
            return findFieldRecursiveImpl(clazz, fieldName);
        } catch (NoSuchFieldException e) {
        }
        return null;
    }

    public static Method findMethodExactIfExists(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        try {
            return findMethodExact(className, classLoader, methodName, parameterTypes);
        } catch (Throwable e) {
            return null;
        }
    }

    public static Method findMethodExact(String className, ClassLoader classLoader, String methodName, Object... parameterTypes) {
        return findMethodExact(findClass(className, classLoader), methodName, getParameterClasses(classLoader, parameterTypes));
    }

    public static Method findMethodExactIfExists(Class<?> clazz, String methodName, Object... parameterTypes) {
        try {
            return findMethodExact(clazz, methodName, getParameterClasses(clazz.getClassLoader(), parameterTypes));
        } catch (Throwable e) {
            XL.e(e);
        }
        return null;
    }

    /**
     * Retrieve classes from an array, where each element might either be a Class
     * already, or a String with the full class name.
     */
    private static Class<?>[] getParameterClasses(ClassLoader classLoader, Object[] parameterTypesAndCallback) {
        Class<?>[] parameterClasses = null;
        for (int i = parameterTypesAndCallback.length - 1; i >= 0; i--) {
            Object type = parameterTypesAndCallback[i];
            if (type == null)
                throw new Error("parameter type must not be null", null);


            if (parameterClasses == null)
                parameterClasses = new Class<?>[i + 1];

            if (type instanceof Class)
                parameterClasses[i] = (Class<?>) type;
            else if (type instanceof String)
                parameterClasses[i] = findClass((String) type, classLoader);
            else
                throw new Error("parameter type must either be specified as Class or String", null);
        }

        // if there are no arguments for the method
        if (parameterClasses == null)
            parameterClasses = new Class<?>[0];

        return parameterClasses;
    }

    public static Method findMethodExact(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        XL.d("XposedHelpers", "inside findMethodExact");

        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            Method method = null;
            try {
                method = clazz.getMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    public static Object newInstance(String clz, Object... args) {
        return newInstance(findClass(clz, null), args);
    }

    public static Object newInstance(Class<?> clazz, Object... args) {
        try {
            return findConstructorBestMatch(clazz, args).newInstance(args);
        } catch (Throwable e) {
            XL.e(e);
        }

        return null;
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Object... args) {
        return findConstructorBestMatch(clazz, getParameterTypes(args));
    }

    public static Constructor<?> findConstructorBestMatch(Class<?> clazz, Class<?>... parameterTypes) {

        try {
            Constructor<?> constructor = findConstructorExact(clazz, parameterTypes);
            return constructor;
        } catch (NoSuchMethodError ignored) {
        }

        Constructor<?> bestMatch = null;
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            // compare name and parameters
            if (ClassUtils.isAssignable(parameterTypes, constructor.getParameterTypes(), true)) {
                // get accessible version of method
                if (bestMatch == null || MemberUtils.compareParameterTypes(
                        constructor.getParameterTypes(),
                        bestMatch.getParameterTypes(),
                        parameterTypes) < 0) {
                    bestMatch = constructor;
                }
            }
        }

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            return bestMatch;
        }
        return null;
    }

    /**
     * Look up a constructor of a class and set it to accessible.
     * See {@link #findMethodExact(String, ClassLoader, String, Object...)} for details.
     */
    public static Constructor<?> findConstructorExact(Class<?> clazz, Class<?>... parameterTypes) {

        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
        }
        return null;
    }

    /**
     * Returns an array with the classes of the given objects.
     */
    public static Class<?>[] getParameterTypes(Object... args) {
        Class<?>[] clazzes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            clazzes[i] = (args[i] != null) ? args[i].getClass() : null;
        }
        return clazzes;
    }

    /** Sets the value of a {@code float} field in the given object instance. A class reference is not sufficient! See also {@link #findField}. */
    public static void setFloatField(Object obj, String fieldName, float value) {
        try {
            findField(obj.getClass(), fieldName).setFloat(obj, value);
        } catch (Throwable e) {
            XL.e(e);
        }
    }

    /**
     * Look up a field in a class and set it to accessible.
     *
     * @param clazz The class which either declares or inherits the field.
     * @param fieldName The field name.
     * @return A reference to the field.
     * @throws NoSuchFieldError In case the field was not found.
     */
    public static Field findField(Class<?> clazz, String fieldName) {

        try {
            Field field = findFieldRecursiveImpl(clazz, fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable e) {
            XL.e(e);
        }
        return null;
    }


    /**
     * Calls an instance or static method of the given object.
     * The method is resolved using {#findMethodBestMatch(Class, String, Object...)}.
     *
     * @param obj The object instance. A class reference is not sufficient!
     * @param methodName The method name.
     * @param args The arguments for the method call.
     * @throws NoSuchMethodError In case no suitable method was found.
     */
    public static Object callMethod(Object obj, String methodName, Object... args) {
        try {
            return findMethodBestMatch(obj.getClass(), methodName, args).invoke(obj, args);
        } catch (Throwable e) {
            XL.e(e);
        }
        return null;
    }

    /**
     * Look up a method in a class and set it to accessible.
     *
     * <p>See { #findMethodBestMatch(Class, String, Class...)} for details. This variant
     * determines the parameter types from the classes of the given objects.
     */
    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Object... args) {
        return findMethodBestMatch(clazz, methodName, getParameterTypes(args));
    }

    /**
     * Look up a method in a class and set it to accessible.
     *
     * <p>This does'nt only look for exact matches, but for the best match. All considered candidates
     * must be compatible with the given parameter types, i.e. the parameters must be assignable
     * to the method's formal parameters. Inherited methods are considered here.
     *
     * @param clazz The class which declares, inherits or overrides the method.
     * @param methodName The method name.
     * @param parameterTypes The types of the method's parameters.
     * @return A reference to the best-matching method.
     * @throws NoSuchMethodError In case no suitable method was found.
     */
    public static Method findMethodBestMatch(Class<?> clazz, String methodName, Class<?>... parameterTypes) {

        try {
            Method method = findMethodExact(clazz, methodName, parameterTypes);
            return method;
        } catch (NoSuchMethodError ignored) {
        }

        Method bestMatch = null;
        Class<?> clz = clazz;
        boolean considerPrivateMethods = true;
        do {
            for (Method method : clz.getDeclaredMethods()) {
                // don't consider private methods of superclasses
                if (!considerPrivateMethods && Modifier.isPrivate(method.getModifiers()))
                    continue;

                // compare name and parameters
                if (method.getName().equals(methodName) && ClassUtils.isAssignable(parameterTypes, method.getParameterTypes(), true)) {
                    // get accessible version of method
                    if (bestMatch == null || MemberUtils.compareParameterTypes(
                            method.getParameterTypes(),
                            bestMatch.getParameterTypes(),
                            parameterTypes) < 0) {
                        bestMatch = method;
                    }
                }
            }
            considerPrivateMethods = false;
        } while ((clz = clz.getSuperclass()) != null);

        if (bestMatch != null) {
            bestMatch.setAccessible(true);
            return bestMatch;
        }
        return null;
    }
}
