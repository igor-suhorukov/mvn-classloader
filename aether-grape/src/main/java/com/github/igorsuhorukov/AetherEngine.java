package com.github.igorsuhorukov;

import groovy.lang.GroovyClassLoader;
import org.springframework.boot.cli.compiler.grape.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 */
public class AetherEngine {

    public static void install(GroovyClassLoader classLoader) {
        install(classLoader, new DependencyResolutionContext(), Collections.singletonList(
                                            new RepositoryConfiguration("central",
                                            URI.create("https://repo.maven.apache.org/maven2/"), false)));
        }

    public static void install(GroovyClassLoader classLoader, DependencyResolutionContext resolutionContext,
                                           List<RepositoryConfiguration> repositoryConfiguration) {

        AetherGrapeEngine grapeEngine = AetherGrapeEngineFactory.create(classLoader,
                repositoryConfiguration, resolutionContext);
        GrapeEngineInstaller.install(grapeEngine);
    }
}
