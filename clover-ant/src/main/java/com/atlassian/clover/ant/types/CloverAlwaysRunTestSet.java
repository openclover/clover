package com.atlassian.clover.ant.types;

import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.Resource;

import java.util.List;

import static org.openclover.util.Lists.newLinkedList;

public class CloverAlwaysRunTestSet extends DataType {
    private List<ResourceCollection> resourceCollections;
    private List<Resource> gatheredResources;

    public void add(ResourceCollection collection) {
        if (isReference()) {
            throw noChildrenAllowed();
        } else {
            getResourceCollections().add(collection);
        }
    }

    private List<Resource> gatherResources() {
        List<Resource> resources = newLinkedList();
        for (ResourceCollection collection : getResourceCollections()) {
            for (Resource resource : collection) {
                resources.add(resource);
            }
        }
        return resources;
    }

    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        } else {
            boolean filesystemOnly = true;
            for (ResourceCollection resources : getResourceCollections()) {
                filesystemOnly = filesystemOnly && resources.isFilesystemOnly();
            }
            return filesystemOnly;
        }
    }

    public List getGatheredResources() {
        if (gatheredResources == null) {
            gatheredResources = gatherResources();
        }
        return gatheredResources;
    }

    private List<ResourceCollection> getResourceCollections() {
        if (resourceCollections == null) {
            resourceCollections = newLinkedList();
        }
        return resourceCollections;
    }

    private CloverAlwaysRunTestSet getRef() {
        return ((CloverAlwaysRunTestSet)getCheckedRef(getProject()));
    }
}
