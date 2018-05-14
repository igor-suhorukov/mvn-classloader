package com.github.igorsuhorukov.url.handler.loadable;

import java.net.URLStreamHandler;

public class LoadableHandlerProvider extends java.net.spi.URLStreamHandlerProvider{

    private final static LoadableURLStreamHandlerFactory LOADABLE_URL_STREAM_HANDLER_FACTORY = new LoadableURLStreamHandlerFactory();
    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if(LoadableURLStreamHandlerFactory.SKIP_LIST.contains(protocol.toLowerCase())){
            return null;
        }
        return LOADABLE_URL_STREAM_HANDLER_FACTORY.createURLStreamHandler(protocol);
    }
}
