package org.openclover.ant.types;

import com.atlassian.clover.optimization.Messages;
import com.atlassian.clover.optimization.Optimizer;

import com.atlassian.clover.api.optimization.OptimizationOptions;
import com.atlassian.clover.optimization.Snapshot;
import com.atlassian.clover.optimization.LocalSnapshotOptimizer;
import org.openclover.ant.tasks.AntInstrumentationConfig;
import org.openclover.runtime.Logger;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.EnumeratedAttribute;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import static org.openclover.util.Lists.newLinkedList;

public class CloverOptimizedTestSet extends BaseCloverOptimizedType implements ResourceCollection {
    private List<ResourceCollection> resourceCollections;
    private List<Resource> optimizedTestResources;
    private CloverAlwaysRunTestSet alwaysRun;

    public void setOrdering(TestOrdering ordering) {
        ordering.applyTo(optionsBuilder);
    }

    public void setMinimize(boolean minimize) {
        this.optionsBuilder.minimize(minimize);
    }

    @Override
    public void setDebug(boolean debug) {
        this.optionsBuilder.debug(debug);
    }

    public void add(CloverAlwaysRunTestSet alwaysRun) {
        this.alwaysRun = alwaysRun;
    }

    public void add(ResourceCollection collection) {
        if (isReference()) {
            throw noChildrenAllowed();
        } else {
            getResourceCollections().add(collection);
        }
    }

    @Override
    public Iterator iterator() {
        if (isReference()) {
            return getRef().iterator();
        } else {
            return getOptimizedTestResource().iterator();
        }
    }

    private List<Resource> optimizeTestResources() {
        Logger originalLogger = takeOverLogging(getProject());
        try {
            AntInstrumentationConfig config = new AntInstrumentationConfig(getProject());
            OptimizationOptions options = this.optionsBuilder.build();
            if (options.isEnabled()) {
                try {
                    final String initString = config.resolveInitString();
                    Optimizer optimizer =
                        new LocalSnapshotOptimizer(
                            optionsBuilder
                                .initString(initString)
                                .snapshot(
                                    snapshotFile == null ? Snapshot.fileForInitString(initString) : snapshotFile).build());

                    List<Resource> resources = getUnderlyingResources();

                    return toResources(
                        optimizer.optimize(
                            alwaysRun == null ? Collections.<TestResource>emptyList() : toTestables(alwaysRun.getGatheredResources()),
                            toTestables(resources)));

                } catch (Exception e) {
                    Logger.getInstance().warn(Messages.noOptimizationBecauseOfException(e), e);
                }
            }

            Logger.getInstance().verbose("Getting underlying test resources");

            return getUnderlyingResources();
        } finally {
            revertLogger(originalLogger);
        }
    }

    private List<Resource> toResources(final List<TestResource> testables) {
        return new LinkedList() {{
            for (TestResource testResource : testables) {
                add(testResource.getResource());
            }
        }};
    }

    private List<TestResource> toTestables(final List<Resource> testables) {
        return new LinkedList() {{
            for (Resource resource : testables) {
                add(new TestResource(resource));
            }
        }};
    }


    private List<Resource> getUnderlyingResources() {
        List<Resource> resources = newLinkedList();
        for (ResourceCollection resourceCollection : getResourceCollections()) {
            for (Resource resource : resourceCollection) {
                resources.add(resource);
            }
        }

        if (alwaysRun != null) {
            resources.addAll(alwaysRun.getGatheredResources());
        }

        OptimizationOptions options = this.optionsBuilder.build();
        //The only ordering we can do if raw resources have been requested is to
        //randomly shuffle them (since we have no data through which we can
        //determine fail fast ordering)
        if (options.isEnabled() && options.isReorderRandomly()) {
            Logger.getInstance().verbose("Randomly shuffling underlying test resources");
            Collections.shuffle(resources);
        }
        return resources;
    }

    @Override
    public int size() {
        if (isReference()) {
            return getRef().size();
        } else {
            return getOptimizedTestResource().size();
        }
    }

    @Override
    public boolean isFilesystemOnly() {
        if (isReference()) {
            return getRef().isFilesystemOnly();
        } else {
            boolean filesystemOnly = true;
            for(ResourceCollection resource : getResourceCollections()) {
                filesystemOnly = filesystemOnly && resource.isFilesystemOnly();
            }
            return filesystemOnly && (alwaysRun == null || alwaysRun.isFilesystemOnly());
        }
    }

    private CloverOptimizedTestSet getRef() {
        return ((CloverOptimizedTestSet)getCheckedRef(getProject()));
    }

    public List<Resource> getOptimizedTestResource() {
        if (optimizedTestResources == null) {
            optimizedTestResources = optimizeTestResources();
        }
        return optimizedTestResources;
    }

    private List<ResourceCollection> getResourceCollections() {
        if (resourceCollections == null) {
            resourceCollections = newLinkedList();
        }
        return resourceCollections;
    }

    public static class TestOrdering extends EnumeratedAttribute {
        public static final String FAILFAST = "failfast";
        public static final String ORIGINAL = "original";
        public static final String RANDOM = "random";

        public TestOrdering() {
        }

        public TestOrdering(String value) {
            setValue(value);
        }

        @Override
        public String[] getValues() {
            return new String[] {FAILFAST, ORIGINAL, RANDOM};
        }

        public OptimizationOptions.Builder applyTo(OptimizationOptions.Builder options) {
            if (FAILFAST.equals(getValue())) {
                return options.reorderFailfast();
            } else if (RANDOM.equals(getValue())) {
                return options.reorderRandomly();
            } else {
                return options.dontReorder();
            }
        }
    }
}
