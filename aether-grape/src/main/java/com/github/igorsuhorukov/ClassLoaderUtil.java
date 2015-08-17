package com.github.igorsuhorukov;

import sun.misc.Unsafe;

/**
 */
public class ClassLoaderUtil {
    private static Class defineClass(ClassLoader classLoader, String className, byte[] bytes) {
        return Unsafe.getUnsafe().defineClass(className, bytes, 0, bytes.length, classLoader, null);
    }
}
