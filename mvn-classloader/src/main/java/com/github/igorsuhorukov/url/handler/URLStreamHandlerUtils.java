package com.github.igorsuhorukov.url.handler;

import com.github.smreed.dropship.ClassLoaderBuilder;
import com.github.smreed.dropship.MavenClassLoader;

import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;

public class URLStreamHandlerUtils {

    public static Collection<URLStreamHandlerFactory> loadURLStreamHandlerFactories(String gav){
        return loadURLStreamHandlerFactories(gav, null);
    }

    public static Collection<URLStreamHandlerFactory> loadURLStreamHandlerFactories(String gav, String repo){
        URLClassLoader mavenClassLoader = getClassLoaderBuilder(repo).forMavenCoordinates(gav);
        Thread currentThread = Thread.currentThread();
        ClassLoader prevContextClassLoader = currentThread.getContextClassLoader();
        currentThread.setContextClassLoader(mavenClassLoader);
        try {
            ServiceLoader<URLStreamHandlerFactory> serviceLoader
                    = ServiceLoader.load(URLStreamHandlerFactory.class, mavenClassLoader);
            Iterator<URLStreamHandlerFactory> iterator = serviceLoader.iterator();
            Collection<URLStreamHandlerFactory> urlStreamHandlerFactories = new ArrayList<URLStreamHandlerFactory>();
            while (iterator.hasNext()) {
                URLStreamHandlerFactory urlStreamHandlerFactory = iterator.next();
                if(urlStreamHandlerFactory!=null){
                    urlStreamHandlerFactories.add(urlStreamHandlerFactory);
                }
            }
            return urlStreamHandlerFactories;
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
