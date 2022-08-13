package com.github.smreed.dropship;

import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

import java.util.List;

class FilterOptionalRuntimeProvided implements DependencyFilter {
    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        Dependency dependency = node.getDependency();
        if ( dependency == null ){
            return true;
        }
        return !dependency.isOptional() && !"provided".equals(dependency.getScope());
    }
}
