package com.github.igorsuhorukov;

import groovy.lang.GroovyClassLoader;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.*;

import java.util.List;

/**
 */
public class AetherEngine {

    public static void install(GroovyClassLoader classLoader) {
        install(classLoader, new DependencyResolutionContext(), RepositoryConfigurationFactory
                .createDefaultRepositoryConfiguration());
    }

    public static void install(GroovyClassLoader classLoader, DependencyResolutionContext resolutionContext,
                                           List<RepositoryConfiguration> repositoryConfiguration) {

        AetherGrapeEngine grapeEngine = AetherGrapeEngineFactory.create(classLoader,
                repositoryConfiguration, resolutionContext);
        GrapeEngineInstaller.install(grapeEngine);
    }
}
