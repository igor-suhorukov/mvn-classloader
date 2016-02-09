package com.github.igorsuhorukov;

import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngine;
import org.springframework.boot.cli.compiler.grape.AetherGrapeEngineFactory;
import org.springframework.boot.cli.compiler.grape.DependencyResolutionContext;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class DefaultAetherGrapeEngine implements GrapeEngine {

    private final AetherGrapeEngine grapeEngine;
    private final GroovyClassLoader groovyClassLoader;

    public DefaultAetherGrapeEngine() {
        groovyClassLoader = getGroovyClassLoader();
        grapeEngine = AetherGrapeEngineFactory.create(groovyClassLoader,
                RepositoryConfigurationFactory.createDefaultRepositoryConfiguration(),
                new DependencyResolutionContext());
    }

    @Override
    public Object grab(String endorsedModule) {
        return grapeEngine.grab(endorsedModule);
    }

    @Override
    public Object grab(Map args) {
        return grapeEngine.grab(args);
    }

    @Override
    public Object grab(Map args, Map... dependencies) {
        return grapeEngine.grab(args, dependencies);
    }

    @Override
    public Map<String, Map<String, List<String>>> enumerateGrapes() {
        return grapeEngine.enumerateGrapes();
    }

    @Override
    public URI[] resolve(Map args, Map... dependencies) {
        return grapeEngine.resolve(args, dependencies);
    }

    @Override
    public URI[] resolve(Map args, List depsInfo, Map... dependencies) {
        return grapeEngine.resolve(args, depsInfo, dependencies);
    }

    @Override
    public Map[] listDependencies(ClassLoader classLoader) {
        return grapeEngine.listDependencies(classLoader);
    }

    @Override
    public void addResolver(Map<String, Object> args) {
        grapeEngine.addResolver(args);
    }

    private static GroovyClassLoader getGroovyClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        GroovyClassLoader classLoader;
        if (contextClassLoader != null && contextClassLoader instanceof GroovyClassLoader) {
            classLoader = (GroovyClassLoader) contextClassLoader;
        } else{
            classLoader = new GroovyClassLoader();
        }
        return classLoader;
    }
}
