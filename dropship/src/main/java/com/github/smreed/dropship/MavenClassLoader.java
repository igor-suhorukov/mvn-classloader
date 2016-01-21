package com.github.smreed.dropship;

import org.eclipse.aether.repository.RemoteRepository;

import java.net.URLClassLoader;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * https://github.com/kaffamobile/dropship
 */
public final class MavenClassLoader {

    /**
   * Creates a classloader that will resolve artifacts against the default "central" repository. Throws
   * {@link IllegalArgumentException} if the GAV is invalid, {@link NullPointerException} if the GAV is null.
   *
   * @param gav artifact group:artifact:version, i.e. joda-time:joda-time:1.6.2
   * @return a classloader that can be used to load classes from the given artifact
   */
  public static URLClassLoader forMavenCoordinates(String gav) {
    return usingCentralRepo().forMavenCoordinates(checkNotNull(gav));
  }

  public static URLClassLoader forMavenCoordinates(String gav, ClassLoader parent) {
    return usingCentralRepo().forMavenCoordinates(checkNotNull(gav), parent);
  }

  public static URLClassLoader forMavenCoordinates(MavenDependency[] mavenDependencies, ClassLoader parent) {
    return usingCentralRepo().forMavenCoordinates(mavenDependencies, parent);
  }

  public static ClassLoaderBuilder using(String url) {
    return new ClassLoaderBuilder(url);
  }

  public static ClassLoaderBuilder usingCentralRepo() {
    RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
    return new ClassLoaderBuilder(central);
  }

  public static ClassLoaderBuilder usingRemoteRepositories(RemoteRepository... repositories) {
    return new ClassLoaderBuilder(repositories);
  }

}
