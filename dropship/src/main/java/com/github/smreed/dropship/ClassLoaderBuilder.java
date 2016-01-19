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
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.AndDependencyFilter;
import org.eclipse.aether.util.filter.PatternExclusionsDependencyFilter;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.springframework.boot.cli.compiler.grape.SettingsXmlRepositorySystemSessionAutoConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.github.smreed.dropship.NotLogger.info;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;

public class ClassLoaderBuilder {

    private static final String COMPILE_SCOPE = "compile";
    private static final ClassLoader SHARE_NOTHING = null;

    private final List<RemoteRepository> repositories;

    ClassLoaderBuilder(String repository) {
        this(new RemoteRepository.Builder("custom", "default", repository).build());
    }

    ClassLoaderBuilder(RemoteRepository... repositories) {
        checkNotNull(repositories);
        checkArgument(repositories.length > 0, "Must specify at least one remote repository.");

        this.repositories = ImmutableList.copyOf(repositories);
    }

    public URLClassLoader forMavenCoordinates(String groupArtifactVersion) {
        try {
            return new URLClassLoader(getArtifactUrls(groupArtifactVersion), SHARE_NOTHING);
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    public URLClassLoader forMavenCoordinates(String groupArtifactVersion, ClassLoader parent) {
        try {
            return new URLClassLoader(getArtifactUrls(groupArtifactVersion), parent);
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    public URLClassLoader forMavenCoordinates(MavenDependency[] dependencies, ClassLoader parent) {
        checkNotNull(dependencies);
        try {
            ArrayList<URL> urls = new ArrayList<URL>();
            for (MavenDependency mavenDependency : dependencies) {
                urls.addAll(getArtifactUrlsCollection(mavenDependency.getGroupArtifactVersion(),
                        mavenDependency.getExcludes()));
            }
            return new URLClassLoader(Iterables.toArray(urls, URL.class), parent);
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    public List<URL> getArtifactUrlsCollection(String groupArtifactVersion, Collection<String> excludes) throws Exception {
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
        return urls;
    }

    private URL[] getArtifactUrls(String groupArtifactVersion) throws Exception {
        return Iterables.toArray(getArtifactUrlsCollection(groupArtifactVersion, null), URL.class);
    }

    protected CollectRequest createCollectRequestForGAV(String gav) {
        DefaultArtifact artifact = new DefaultArtifact(gav);
        Dependency dependency = new Dependency(artifact, getScope());

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        for (RemoteRepository repository : repositories) {
            collectRequest.addRepository(repository);
        }

        return collectRequest;
    }

    protected List<Artifact> collectDependenciesIntoArtifacts(CollectRequest collectRequest, Collection<String> excludes)
            throws PlexusContainerException, ComponentLookupException, DependencyCollectionException,
            ArtifactResolutionException, DependencyResolutionException {

        RepositorySystem repositorySystem = newRepositorySystem();
        RepositorySystemSession session = newSession(repositorySystem);
        RepositoryUtils.configureRepositories(collectRequest, session);

        DependencyNode node = repositorySystem.collectDependencies(session, collectRequest).getRoot();

        DependencyFilter filter;
        if (excludes != null && !excludes.isEmpty()) {
            filter = new AndDependencyFilter(new ScopeDependencyFilter(), new PatternExclusionsDependencyFilter(excludes));
        } else {
            filter = new ScopeDependencyFilter();
        }

        DependencyRequest request = new DependencyRequest(node, filter);

        repositorySystem.resolveDependencies(session, request);

        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        node.accept(nlg);

        return nlg.getArtifacts(false);
    }

    protected RepositorySystem newRepositorySystem() throws PlexusContainerException, ComponentLookupException {
        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositorySystem.class, DefaultRepositorySystem.class);
        locator.addService(RepositoryConnectorFactory.class,
                BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        return locator.getService(RepositorySystem.class);
    }

    protected RepositorySystemSession newSession(RepositorySystem system) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        applySettings(system, session);
        session.setRepositoryListener(new LoggingRepositoryListener());
        return session;
    }

    protected void applySettings(RepositorySystem system, DefaultRepositorySystemSession session) {
        if(!Boolean.getBoolean("skipMavenSettings")){
            SettingsXmlRepositorySystemSessionAutoConfiguration autoConfiguration =
                    new SettingsXmlRepositorySystemSessionAutoConfiguration();
            autoConfiguration.apply(session, system);
        }
        if (session.getLocalRepositoryManager() == null) {
            LocalRepository localRepository = new LocalRepository(Settings.localRepoPath());
            LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(session, localRepository);
            session.setLocalRepositoryManager(localRepositoryManager);
        }
    }

    protected String getScope() {
        return COMPILE_SCOPE;
    }

    public URL resolveArtifact(String gav) throws PlexusContainerException, ComponentLookupException,
            ArtifactResolutionException, MalformedURLException {

        RepositorySystem repositorySystem = newRepositorySystem();
        RepositorySystemSession session = newSession(repositorySystem);
        ArtifactRequest artifactRequest = new ArtifactRequest();
        for (RemoteRepository repository : repositories) {
            artifactRequest.addRepository(RepositoryUtils.applySessionSettingsToRepository(session, repository));
        }
        artifactRequest.setArtifact(new DefaultArtifact(gav));
        ArtifactResult artifactResult = repositorySystem.resolveArtifact(session, artifactRequest);
        return artifactResult.getArtifact().getFile().toURI().toURL();
    }
}
