package com.github.igorsuhorukov.maven;

import com.github.smreed.dropship.ClassLoaderBuilder;
import com.github.smreed.dropship.MavenClassLoader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MavenDependenciesResolver implements DependenciesResolver {

    @Override
    public List<URL> resolve(Set<String> dependenciesUrl) {
        final ClassLoaderBuilder classLoaderBuilder = MavenClassLoader.usingCentralRepo();
        final ArrayList<URL> classPath = new ArrayList<>();
        dependenciesUrl.stream().filter(Objects::nonNull).filter(url -> url.startsWith("mvn:/")).map(dependency -> {
            try {
                String[] protocol = dependency.replace("mvn:/", "").split("\\?");
                if (protocol.length == 2) {
                    String repository = protocol[1];
                    String artifact = protocol[0];
                    return MavenClassLoader.using(repository).getArtifactUrlsCollection(artifact, null);
                } else {
                    return classLoaderBuilder.getArtifactUrlsCollection(protocol[0], null);
                }
            } catch (Exception e) {
                throw new RuntimeException();
            }
        }).forEach(classPath::addAll);
        return classPath;
    }
}
