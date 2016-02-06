package com.github.igorsuhorukov.url.handler;

import com.github.igorsuhorukov.url.handler.loadable.LoadableURLStreamHandlerFactory;
import com.github.igorsuhorukov.url.handler.mvn.MavenURLStreamHandlerFactory;

import java.net.URLStreamHandlerFactory;

public class UniversalURLStreamHandlerFactory extends ChainURLStreamHandlerFactory {
    public UniversalURLStreamHandlerFactory() {
        super(new URLStreamHandlerFactory[]{new MavenURLStreamHandlerFactory(), new LoadableURLStreamHandlerFactory()});
    }
}
