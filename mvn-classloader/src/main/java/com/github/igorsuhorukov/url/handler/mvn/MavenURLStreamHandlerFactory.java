package com.github.igorsuhorukov.url.handler.mvn;

import com.github.smreed.dropship.ClassLoaderBuilder;
import com.github.smreed.dropship.MavenClassLoader;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Support URL syntax for maven repository artifacts.
 *
 * For example mvn:/com.github.igor-suhorukov:aspectj-scripting:pom:1.3?https://jcenter.bintray.com
 */
public class MavenURLStreamHandlerFactory implements java.net.URLStreamHandlerFactory{

    public static final String MVN_PROTOCOL = "mvn";

    protected java.net.URLStreamHandlerFactory urlStreamHandlerFactory;

    public MavenURLStreamHandlerFactory() {
    }

    public MavenURLStreamHandlerFactory(java.net.URLStreamHandlerFactory urlStreamHandlerFactory) {
        this.urlStreamHandlerFactory = urlStreamHandlerFactory;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (MVN_PROTOCOL.equals(protocol)) return new URLStreamHandler() {
            protected URLConnection openConnection(URL url) throws IOException {
                try {
                    String repository = url.getQuery();
                    ClassLoaderBuilder classLoaderBuilder;
                    if (repository == null){
                        classLoaderBuilder = MavenClassLoader.usingCentralRepo();
                    } else {
                        classLoaderBuilder = MavenClassLoader.using(repository);
                    }
                    return classLoaderBuilder.resolveArtifact(url.getPath()).openConnection();
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };
        else{
            if(urlStreamHandlerFactory !=null){
                return urlStreamHandlerFactory.createURLStreamHandler(protocol);
            } else {
                return null;
            }
        }
    }
}
