package com.github.smreed.dropship;

import java.util.Collection;

/**
 */
public class MavenDependency {
    private String groupArtifactVersion;
    private Collection<String> excludes;

    public MavenDependency(String groupArtifactVersion) {
        if(groupArtifactVersion==null || groupArtifactVersion.isEmpty()){
            throw new IllegalArgumentException("groupArtifactVersion is empty");
        }
        this.groupArtifactVersion = groupArtifactVersion;
    }

    public MavenDependency(String groupArtifactVersion, Collection<String> excludes) {
        this(groupArtifactVersion);
        if(excludes!=null && !excludes.isEmpty()) {
            this.excludes = excludes;
        }
    }

    public String getGroupArtifactVersion() {
        return groupArtifactVersion;
    }

    public Collection<String> getExcludes() {
        return excludes;
    }
}
