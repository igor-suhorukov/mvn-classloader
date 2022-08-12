package com.github.smreed.dropship;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyResolutionException;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.github.smreed.dropship.NotLogger.info;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * https://github.com/kaffamobile/dropship
 */
public final class Dropship {

  private static ClassLoaderBuilder classLoaderBuilder() {
    Optional<String> override = Settings.mavenRepoUrl();
    if (override.isPresent()) {
      info("Will load artifacts from %s", override);
      return MavenClassLoader.using(override.get());
    } else {
      return MavenClassLoader.usingCentralRepo();
    }
  }

  private static String resolveGav(String gav) {
    ImmutableList<String> tokens = ImmutableList.copyOf(Settings.GAV_SPLITTER.split(gav));

    checkArgument(tokens.size() > 1, "Require groupId:artifactId[:version]");
    checkArgument(tokens.size() < 4, "Require groupId:artifactId[:version]");

    if (tokens.size() > 2) {
      return gav;
    }

    Properties settings = Settings.loadBootstrapPropertiesUnchecked();

    if (settings.containsKey(gav)) {
      return Settings.GAV_JOINER.join(tokens.get(0), tokens.get(1), settings.getProperty(gav));
    } else {
      return Settings.GAV_JOINER.join(tokens.get(0), tokens.get(1), "[0,)");
    }
  }

  public static void main(String[] args) throws Exception {
    checkNotNull(args);
    checkArgument(args.length >= 2, "Must specify groupId:artifactId[:version] and classname!");

    info("Starting Dropship v%s", Settings.dropshipVersion());

    String artifactLocator = args[0];
    MavenDependency[] requestedDependencies = getMavenDependencies(artifactLocator);
    if("justPrintDependencies".equals(args[1])){
      List<URL> urls = classLoaderBuilder().getUrls(requestedDependencies);
      urls.forEach(url -> {
        System.out.println(url.toExternalForm());
      });
      return;
    }

    URLClassLoader loader = classLoaderBuilder().forMavenCoordinates(requestedDependencies, ClassLoader.getPlatformClassLoader());

    System.setProperty("dropship.running", "true");

    Class<?> mainClass = loader.loadClass(args[1]);

    Thread.currentThread().setContextClassLoader(loader);

    Method mainMethod = mainClass.getMethod("main", String[].class);

    Iterable<String> mainArgs = Iterables.skip(ImmutableList.copyOf(args), 2);
    mainMethod.invoke(null, (Object) Iterables.toArray(mainArgs, String.class));
  }

  private static MavenDependency[] getMavenDependencies(String artifactLocator) {
    List<MavenDependency> dependencies = new ArrayList<>();
    String[] artifactGavItem = artifactLocator.split("\\|");
    for(String gavString: artifactGavItem){
      String gav = resolveGav(gavString);
      info("Requested %s, will load artifact and dependencies for %s.", gavString, gav);
      dependencies.add(new MavenDependency(gav));
    }
    MavenDependency[] requestedDependencies = dependencies.toArray(new MavenDependency[0]);
    return requestedDependencies;
  }

  public static String[] resolveAndReturnFiles(String artifactLocator) throws PlexusContainerException,
          DependencyCollectionException, ArtifactResolutionException, MalformedURLException,
          ComponentLookupException, DependencyResolutionException {
    MavenDependency[] requestedDependencies = getMavenDependencies(artifactLocator);
    return classLoaderBuilder().getUrls(requestedDependencies).stream().map(URL::toExternalForm).toArray(String[]::new);
  }
}
