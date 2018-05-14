package com.github.igorsuhorukov.url.handler.loadable;

import com.github.igorsuhorukov.service.MavenServiceLoader;
import com.github.igorsuhorukov.url.handler.ChainURLStreamHandlerFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * URLStreamHandlerFactory based on loadable maven modules
 */
public class LoadableURLStreamHandlerFactory implements java.net.URLStreamHandlerFactory{

    final static private Logger logger = Logger.getLogger(LoadableURLStreamHandlerFactory.class.getName());
    public static final String HANDLER_FACTORT_ARTIFACT = "com.github.igor-suhorukov:%s-url-handler:LATEST";
    static final Set<String> SKIP_LIST = new TreeSet<String>(){{
        add("mvn");add("file");add("jar");add("http");add("https");add("ftp");add("mailto");}};

    private static LoadingCache<String, URLStreamHandlerFactory> urlStreamHandlerFactoryLoadingCache = CacheBuilder
            .newBuilder().build(new CacheLoader<String, URLStreamHandlerFactory>() {
        @Override
        public URLStreamHandlerFactory load(String protocol) throws Exception {
            return loadUrlStreamHandlerFactory(protocol);
        }
    });

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if(protocol!=null && !SKIP_LIST.contains(protocol.toLowerCase())) {
            try {
                URLStreamHandlerFactory urlStreamHandlerFactory = urlStreamHandlerFactoryLoadingCache.get(protocol);
                return urlStreamHandlerFactory!=null ? urlStreamHandlerFactory.createURLStreamHandler(protocol) : null;
            } catch (ExecutionException e) {
                logger.severe(e.getMessage());
            }
        }
        return null;
    }

    static URLStreamHandlerFactory loadUrlStreamHandlerFactory(String protocol) {
        String artifact = System.getProperty(protocol+"MvnUrlHandler",String.format(HANDLER_FACTORT_ARTIFACT,protocol));

        Collection<URLStreamHandlerFactory> urlStreamHandlerFactories = MavenServiceLoader.loadServices(
                artifact, URLStreamHandlerFactory.class);

        if(urlStreamHandlerFactories.isEmpty()){
            return null;
        } else if(urlStreamHandlerFactories.size() == 1){
            return urlStreamHandlerFactories.iterator().next();
        } else {
            URLStreamHandlerFactory[] streamHandlerFactories = urlStreamHandlerFactories.toArray(
                    new URLStreamHandlerFactory[urlStreamHandlerFactories.size()]);
            return new ChainURLStreamHandlerFactory(streamHandlerFactories);
        }
    }
}
