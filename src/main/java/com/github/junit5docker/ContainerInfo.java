package com.github.junit5docker;

import java.util.Collection;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;

class ContainerInfo {

    private final String containerId;

    private final Collection<String> networkIds;

    ContainerInfo(String containerId, Collection<String> networkIds) {
        this.containerId = containerId;
        this.networkIds = networkIds == null ? emptySet() : unmodifiableCollection(networkIds);
    }

    String getContainerId() {
        return containerId;
    }

    Collection<String> getNetworkIds() {
        return networkIds;
    }
}
