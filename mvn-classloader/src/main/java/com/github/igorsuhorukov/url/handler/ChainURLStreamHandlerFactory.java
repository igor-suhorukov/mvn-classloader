package com.github.igorsuhorukov.url.handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Logger;

/**
 * Class allow to make chain of URLStreamHandlerFactory invocations
 */
public class ChainURLStreamHandlerFactory implements URLStreamHandlerFactory{

    final static private Logger logger = Logger.getLogger(ChainURLStreamHandlerFactory.class.getName());

    public URLStreamHandlerFactory[] streamHandlerFactories;

    public ChainURLStreamHandlerFactory(URLStreamHandlerFactory[] streamHandlerFactories) {
        validateInputChain(streamHandlerFactories);
        this.streamHandlerFactories = streamHandlerFactories;
    }

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        for(URLStreamHandlerFactory urlStreamHandlerFactory: streamHandlerFactories){
            URLStreamHandler urlStreamHandler = null;
            try {
                urlStreamHandler = urlStreamHandlerFactory.createURLStreamHandler(protocol);
            } catch (Exception ignore) {
                logger.warning(ignore.getMessage());
            }
            if(urlStreamHandler!=null){
                return urlStreamHandler;
            }
        }
        return null;
    }

    private static void validateInputChain(URLStreamHandlerFactory[] streamHandlerFactories) {
        if(streamHandlerFactories==null || streamHandlerFactories.length==0){
            throw new IllegalArgumentException("Empty URLStreamHandlerFactories array");
        }
        for (int i = 0; i < streamHandlerFactories.length; i++) {
            URLStreamHandlerFactory urlStreamHandlerFactory = streamHandlerFactories[i];
            if (urlStreamHandlerFactory == null) {
                throw new IllegalArgumentException("Null element. Index ="+i);
            }
        }
    }
}
