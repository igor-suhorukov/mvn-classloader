package com.github.igorsuhorukov.service;

import com.github.smreed.dropship.ClassLoaderBuilder;
import com.github.smreed.dropship.MavenClassLoader;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;

public class MavenServiceLoader<T> {

    public static<T> Collection<T> loadServices(String gav, Class<T> serviceClass){
        return loadServices(gav, null, serviceClass);
    }

    public static<T> Collection<T> loadServices(String gav, String repo, Class<T> serviceClass){
        URLClassLoader mavenClassLoader = getClassLoaderBuilder(repo).forMavenCoordinates(gav);
        return getService(mavenClassLoader, serviceClass);
    }

    public static <T> Collection<T> getService(ClassLoader classLoader, Class<T> serviceClass) {
        Thread currentThread = Thread.currentThread();
        ClassLoader prevContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(classLoader);
        try {
            ServiceLoader<T> serviceLoader = ServiceLoader.load(serviceClass, classLoader);
            Iterator<T> iterator = serviceLoader.iterator();
            Collection<T> services = new ArrayList<T>();
            while (iterator.hasNext()) {
                T service = iterator.next();
                if(service!=null){
                    services.add(service);
                }
            }
            return services;
        } finally {
            currentThread.setContextClassLoader(prevContextClassLoader);
        }
    }

    private static ClassLoaderBuilder getClassLoaderBuilder(String repo) {
        ClassLoaderBuilder classLoaderBuilder;
        if (repo!=null && !repo.isEmpty()){
            classLoaderBuilder = MavenClassLoader.using(repo);
        } else {
            classLoaderBuilder = MavenClassLoader.usingCentralRepo();
        }
        return classLoaderBuilder;
    }
}
