package com.github.smreed.dropship;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultRepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

import static com.github.smreed.dropship.NotLogger.info;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;


/**
 * https://github.com/kaffamobile/dropship
 */
public final class MavenClassLoader {

  public static class ClassLoaderBuilder {

    private static final String COMPILE_SCOPE = "compile";
    private static final ClassLoader SHARE_NOTHING = null;

    private final List<RemoteRepository> repositories;
    private final File localRepositoryDirectory;

    private ClassLoaderBuilder(RemoteRepository... repositories) {
      checkNotNull(repositories);
      checkArgument(repositories.length > 0, "Must specify at least one remote repository.");

      this.repositories = ImmutableList.copyOf(repositories);
      this.localRepositoryDirectory = new File(Settings.localRepoPath());
    }

    public URLClassLoader forMavenCoordinates(String groupArtifactVersion) {
      try {
          return new URLClassLoader(getArtifactUrls(groupArtifactVersion), SHARE_NOTHING);
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    public URLClassLoader forMavenCoordinatesShared(String groupArtifactVersion) {
      try {
          ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
          return new URLClassLoader(getArtifactUrls(groupArtifactVersion),
                  contextClassLoader != null ? contextClassLoader : MavenClassLoader.class.getClassLoader());
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    public URL[] getArtifactUrls(String groupArtifactVersion) throws Exception {
        return getArtifactUrls(groupArtifactVersion, null);
    }

    public URL[] getArtifactUrls(String groupArtifactVersion, Collection<String> excludes) throws Exception {
      info("Collecting maven metadata.");
      CollectRequest collectRequest = createCollectRequestForGAV(groupArtifactVersion);

      info("Resolving dependencies.");
      List<Artifact> artifacts = collectDependenciesIntoArtifacts(collectRequest, excludes);

      info("Building classpath for %s from %d URLs.", groupArtifactVersion, artifacts.size());
      List<URL> urls = Lists.newArrayListWithExpectedSize(artifacts.size());
      for (Artifact artifact : artifacts) {
        urls.add(artifact.getFile().toURI().toURL());
      }

      for (String path : Settings.additionalClasspathPaths()) {
        info("Adding \"%s\" to classpath.", path);
        urls.add(new File(path).toURI().toURL());
      }

      return Iterables.toArray(urls, URL.class);
    }

      private CollectRequest createCollectRequestForGAV(String gav) {
      DefaultArtifact artifact = new DefaultArtifact(gav);
      Dependency dependency = new Dependency(artifact, COMPILE_SCOPE);

      CollectRequest collectRequest = new CollectRequest();
      collectRequest.setRoot(dependency);
      for (RemoteRepository repository : repositories) {
        collectRequest.addRepository(repository);
      }

      return collectRequest;
    }

    private List<Artifact> collectDependenciesIntoArtifacts(CollectRequest collectRequest, Collection<String> excludes)
      throws PlexusContainerException, ComponentLookupException, DependencyCollectionException, ArtifactResolutionException, DependencyResolutionException {

      RepositorySystem repositorySystem = newRepositorySystem();
      RepositorySystemSession session = newSession(repositorySystem);
      DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();

      DependencyFilter filter;
      if(excludes!=null && !excludes.isEmpty()){
          filter = new AndDependencyFilter(new ScopeDependencyFilter(), new ExclusionsDependencyFilter(excludes));
      } else {
          filter = new ScopeDependencyFilter();
      }

      DependencyRequest request = new DependencyRequest(node, filter);

      repositorySystem.resolveDependencies(session, request);

      PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
      node.accept(nlg);

      return nlg.getArtifacts(false);
    }

    private RepositorySystem newRepositorySystem() throws PlexusContainerException, ComponentLookupException {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositorySystem.class, DefaultRepositorySystem.class);
        locator.addService(RepositoryConnectorFactory.class,
                BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    private RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        if (session.getLocalRepositoryManager() == null) {
            LocalRepository localRepository = new LocalRepository(Settings.localRepoPath());
            LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(session, localRepository);
            session.setLocalRepositoryManager(localRepositoryManager);
        }
        session.setRepositoryListener(new LoggingRepositoryListener());
        return session;
    }
  }

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

  public static URLClassLoader forMavenCoordinatesShared(String gav) {
    return usingCentralRepo().forMavenCoordinatesShared(checkNotNull(gav));
  }

  public static ClassLoaderBuilder using(String url) {
    RemoteRepository custom = new RemoteRepository.Builder("custom", "default", url).build();
    return new ClassLoaderBuilder(custom);
  }

  public static ClassLoaderBuilder usingCentralRepo() {
    RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build();
    return new ClassLoaderBuilder(central);
  }

}
