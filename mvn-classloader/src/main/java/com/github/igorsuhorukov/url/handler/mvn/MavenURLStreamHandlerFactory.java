package com.github.igorsuhorukov.url.handler.mvn;

import java.net.URLStreamHandler;
import java.util.logging.Logger;

/**
 * Support URL syntax for maven repository artifacts.
 *
 * For example mvn:/com.github.igor-suhorukov:aspectj-scripting:pom:1.3?https://jcenter.bintray.com
 */
public class MavenURLStreamHandlerFactory implements java.net.URLStreamHandlerFactory{

    private static final Logger logger = Logger.getLogger(MavenURLStreamHandlerFactory.class.getName());

    public static final String MVN_PROTOCOL = "mvn";

    protected java.net.URLStreamHandlerFactory urlStreamHandlerFactory;

    public MavenURLStreamHandlerFactory() {
    }

    public MavenURLStreamHandlerFactory(java.net.URLStreamHandlerFactory urlStreamHandlerFactory) {
        this.urlStreamHandlerFactory = urlStreamHandlerFactory;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (MVN_PROTOCOL.equals(protocol)){
            return new Handler();
        } else{
            if(urlStreamHandlerFactory !=null){
                try {
                    return urlStreamHandlerFactory.createURLStreamHandler(protocol);
                } catch (Exception ignore) {
                    logger.warning(ignore.getMessage());
                    return null;
                }
            } else {
                return null;
            }
        }
    }
}
