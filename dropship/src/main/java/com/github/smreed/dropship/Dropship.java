package com.github.smreed.dropship;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static com.github.smreed.dropship.NotLogger.info;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * https://github.com/kaffamobile/dropship
 */
public final class Dropship {

  public static final String DEPENDENCY_PATTERN = "groupId:artifactId[:extension[:classifier]]:version";

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

    checkArgument(tokens.size() > 1, "Require " + DEPENDENCY_PATTERN);
    checkArgument(tokens.size() < 6, "Require " + DEPENDENCY_PATTERN);

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
    checkArgument(args.length >= 2, "Must specify " + DEPENDENCY_PATTERN + " and classname psvm 'main' method!");

    info("Starting Dropship v%s", Settings.dropshipVersion());

    String artifactLocator = args[0];
    if("justPrintDependencies".equals(args[1])){
      String[] urls = resolveAndReturnFiles(artifactLocator);
      Arrays.asList(urls).forEach(System.out::println);
      return;
    }

    MavenDependency[] requestedDependencies = getMavenDependencies(artifactLocator);
    URLClassLoader loader = classLoaderBuilder().forMavenCoordinates(requestedDependencies, ClassLoader.getPlatformClassLoader());

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

  public static String[] resolveAndReturnFiles(String artifactLocator) {
    MavenDependency[] requestedDependencies = getMavenDependencies(artifactLocator);
    try {
      return classLoaderBuilder().getUrls(requestedDependencies).stream().map(URL::toExternalForm).toArray(String[]::new);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
