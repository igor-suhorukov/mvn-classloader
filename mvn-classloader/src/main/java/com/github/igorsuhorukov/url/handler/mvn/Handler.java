package com.github.igorsuhorukov.url.handler.mvn;

import com.github.smreed.dropship.ClassLoaderBuilder;
import com.github.smreed.dropship.MavenClassLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        try {
            String repository = url.getQuery();
            ClassLoaderBuilder classLoaderBuilder;
            if (repository == null){
                classLoaderBuilder = MavenClassLoader.usingCentralRepo();
            } else {
                classLoaderBuilder = MavenClassLoader.using(repository);
            }
            return classLoaderBuilder.resolveArtifact(getUrlPath(url)).openConnection();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private static String getUrlPath(URL url) {
        if(StringUtils.hasText(url.getPath()) && url.getPath().startsWith("/")){
            return url.getPath().substring(1);
        } else {
            return url.getPath();
        }
    }
}
