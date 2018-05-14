package com.github.igorsuhorukov.url.handler.mvn;

import java.net.URLStreamHandler;

public class MavenHandlerProvider extends java.net.spi.URLStreamHandlerProvider {

    private final static MavenURLStreamHandlerFactory MAVEN_URL_STREAM_HANDLER_FACTORY =
            new MavenURLStreamHandlerFactory();

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return MAVEN_URL_STREAM_HANDLER_FACTORY.createURLStreamHandler(protocol);
    }
}
