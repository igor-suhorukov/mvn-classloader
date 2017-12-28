package com.github.igorsuhorukov.url;

import org.codehaus.plexus.util.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class UrlBootstrapLoader {

    private static final String MAIN_CLASS = "Main-Class";
    private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";

    public static void main(String[] args) throws Exception{
        String artifactUrl = System.getProperty("artifactUrl");
        String artifactUrlReference = System.getProperty("artifactUrlReference");
        if(artifactUrlReference!=null && !artifactUrlReference.isEmpty()){
            if(artifactUrl!=null && !artifactUrl.isEmpty()){
                throw new IllegalArgumentException("use either artifactUrl or artifactUrlReference system property");
            }
            artifactUrl = getReference(artifactUrlReference);
        }
        if(artifactUrl==null || artifactUrl.isEmpty()){
            throw new IllegalArgumentException("System property artifactUrl is empty");
        }
        URL artifactJavaUrl = new URL(artifactUrl);
        try (URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{artifactJavaUrl})){
            String mainClass = getMainClassFromManifest(artifactUrl, artifactJavaUrl);
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(urlClassLoader);
            try {
                invokeMainMethod(urlClassLoader, mainClass, args);
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    private static String getMainClassFromManifest(String artifactUrl, URL artifactJavaUrl) throws IOException {
        try (JarInputStream manifestStream = new JarInputStream(artifactJavaUrl.openStream())){
            Manifest manifest = manifestStream.getManifest();
            if(manifest==null){
                throw new IllegalArgumentException(MANIFEST_MF +" not found in " + artifactUrl);
            }
            Attributes mainAttributes = manifest.getMainAttributes();
            if(mainAttributes==null){
                throw new IllegalArgumentException(MANIFEST_MF+" is empty");
            }
            String mainClass = mainAttributes.getValue(MAIN_CLASS);
            if(mainClass==null || mainClass.isEmpty()){
                throw new IllegalArgumentException(MAIN_CLASS+" attribute not found in "+MANIFEST_MF);
            }
            return mainClass;
        }
    }

    private static void invokeMainMethod(URLClassLoader urlClassLoader, String mainClassName, String[] args){
        try {
            Class<?> mainClass = urlClassLoader.loadClass(mainClassName);
            Method mainMethod = mainClass.getDeclaredMethod("main", new Class[]{String[].class});
            mainMethod.invoke((Object)null, new Object[]{args});
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static String getReference(String artifactUrlReference) throws IOException {
        URL referenceUrl = new URL(artifactUrlReference);
        InputStream referenceUrlStream = referenceUrl.openStream();
        String reference = IOUtil.toString(referenceUrlStream);
        if(reference==null || reference.isEmpty() || reference.contains("\n")){
            throw new IllegalArgumentException("artifactUrlReference resource should be non empty");
        }
        return reference;
    }
}
