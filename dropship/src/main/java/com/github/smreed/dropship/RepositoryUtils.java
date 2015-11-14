package com.github.smreed.dropship;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.ArrayList;
import java.util.List;

public class RepositoryUtils {

    public static void configureRepositories(CollectRequest collectRequest, RepositorySystemSession session) {
        List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
        for(RemoteRepository repository: collectRequest.getRepositories()){
            repositories.add(applyAuthentication(applyProxy(applyMirror(repository, session), session), session));
        }
        collectRequest.setRepositories(repositories);
    }

    static RemoteRepository applyMirror(RemoteRepository repository, RepositorySystemSession session) {
        RemoteRepository mirror = session.getMirrorSelector().getMirror(repository);
        return mirror != null ? mirror : repository;
    }

    static RemoteRepository applyProxy(RemoteRepository repository, RepositorySystemSession session) {
        if (repository.getProxy() == null) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(repository);
            builder.setProxy(session.getProxySelector().getProxy(repository));
            repository = builder.build();
        }
        return repository;
    }

    static RemoteRepository applyAuthentication(RemoteRepository repository, RepositorySystemSession session) {
        if (repository.getAuthentication() == null) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(repository);
            builder.setAuthentication(session.getAuthenticationSelector()
                    .getAuthentication(repository));
            repository = builder.build();
        }
        return repository;
    }

}
