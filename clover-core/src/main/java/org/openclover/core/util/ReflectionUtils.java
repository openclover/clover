package org.openclover.core.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class ReflectionUtils {
    public static <T> T invokeVirtualImplicit(String method, Object onObj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return ReflectionUtils.<T>invokeVirtualImplicit(method, onObj, new Object[] {});
    }

    /**
     * Invoke any method on a given object that has the given parameters.
     * If method is not found on the Object's class, then all superclasses are searched until a method is found.
     * Currently not supported:
     * <ul>
     * <li> primitive argument types </li>
     * <li> null arguments  </li>
     * </ul>
     *
     * @param methodName the name of the method to invoke.
     * @param onObj the object to invoke the method on
     * @param withArgs the arguments to pass
     * @throws NoSuchMethodException if method can not found onObj
     * @throws java.lang.reflect.InvocationTargetException if an error occurs when invoking the method
     * @return the return value of the method that was invoked
     */
    @SuppressWarnings("unchecked")
    public static <T> T invokeVirtualImplicit(String methodName, Object onObj, Object... withArgs) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Object[] argTypesAndNames = classAndNamesFor(withArgs);

        Class<?> clazz = onObj.getClass();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (matches(method, methodName, withArgs)) {
                    method.setAccessible(true);
                    return (T)method.invoke(onObj, withArgs);
                }
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException("No method found called " + methodName + " accomodating arguments " + Arrays.toString((String[])argTypesAndNames[1]) + " on " + onObj.getClass() + " or its base classes");
    }

    public static <T> T invokeVirtual(String methodName, Object onObj, Class<?>[] declaredArgTypes, Object... withArgs) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?> clazz = onObj.getClass();
        while (clazz != null) {
            final Method method = clazz.getDeclaredMethod(methodName, declaredArgTypes);
            if (method != null) {
                method.setAccessible(true);
                return (T)method.invoke(onObj, withArgs);
            }
            clazz = clazz.getSuperclass();
        }
        throw new NoSuchMethodException("No method found called " + methodName + " accomodating arguments " + Arrays.toString(declaredArgTypes) + " on " + onObj.getClass() + " or its base classes");
    }

    private static boolean matches(Method method, String methodName, Object[] withArgs) {
        if (!method.getName().equals(methodName)) {
            return false;
        }

        final Class<?>[] paramTypes = method.getParameterTypes();

        if (paramTypes.length != withArgs.length) {
            return false;
        }

        for(int i = 0; i < paramTypes.length; i++) {
            if (!paramTypes[i].isAssignableFrom(withArgs[i] == null ? paramTypes[i] : withArgs[i].getClass())) {
                return false;
            }
        }

        return true;
    }

    private static Object[][] classAndNamesFor(Object[] args) {
        Class<?>[] argTypes = new Class<?>[args.length];
        String[] argNames = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            Object withArg = args[i];
            argTypes[i] = withArg.getClass();
            argNames[i] = withArg.getClass().getCanonicalName();
        }
        return new Object[][] { argTypes, argNames};
    }
}
