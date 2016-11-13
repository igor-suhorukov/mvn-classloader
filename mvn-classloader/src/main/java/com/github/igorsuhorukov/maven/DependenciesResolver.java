package com.github.igorsuhorukov.maven;

import java.net.URL;
import java.util.List;
import java.util.Set;

public interface DependenciesResolver {
    List<URL> resolve(Set<String> dependenciesUrl);
}
