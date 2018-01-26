package com.github.junit5docker;

import java.util.Collection;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;

public class ContainerInfo {

    private final String containerId;
    private final Collection<String> networkIds;

    public ContainerInfo(String containerId, Collection<String> networkIds) {
        this.containerId = containerId;
        this.networkIds = networkIds == null ? emptySet() : unmodifiableCollection(networkIds);
    }

    public String getContainerId() {
        return containerId;
    }

    public Collection<String> getNetworkIds() {
        return networkIds;
    }
}
