package org.openclover.ant.types;

import org.apache.tools.ant.types.Resource;
import org.openclover.core.api.optimization.Optimizable;

class TestResource implements Optimizable {
    private final Resource resource;

    TestResource(Resource resource) {
        this.resource = resource;
    }

    @Override
    public String getName() {
        return BaseCloverOptimizedType.normalizePath(resource.getName());
    }

    Resource getResource() {
        return resource;
    }
}
