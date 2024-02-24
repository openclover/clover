package org.openclover.core.registry;

import org.openclover.core.api.instrumentation.ConcurrentInstrumentationException;
import org.openclover.core.api.registry.CoverageDataProvider;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.PackageInfo;
import org.openclover.core.instr.InstrumentationSessionImpl;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullPackageInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.api.registry.HasMetricsFilter;
import org.openclover.core.util.Path;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.openclover.core.util.Lists.newLinkedList;

public interface ProjectView extends InstrumentationTarget {
    ProjectView NONE = new ProjectView() {
        @Override
        public FullProjectInfo getProject() { return null; }
        @Override
        public RegistryUpdate applyUpdate(long expectedVersion, InstrumentationSessionImpl.Update update) { return null; }
        @Override
        public void resolve(Path sourcePath) {}
    };

    FullProjectInfo getProject();
    void resolve(Path sourcePath);

    class Original implements ProjectView {
        private final AtomicLong version;
        private final FullProjectInfo project;
        private final Collection<Filtered> filteredViews;

        public Original(FullProjectInfo project) {
            this.version = new AtomicLong(project.getVersion());
            this.project = project;
            this.filteredViews = newLinkedList();
        }

        @Override
        public FullProjectInfo getProject() {
            return project;
        }

        public Filtered newProjection(HasMetricsFilter.Invertable filter) {
            final Filtered filteredView = new Filtered(filter, project);
            filteredViews.add(filteredView);
            return filteredView;
        }

        @Override
        @SuppressWarnings("unchecked")
        public RegistryUpdate applyUpdate(long expectedVersion, InstrumentationSessionImpl.Update update) throws ConcurrentInstrumentationException {
            if (!version.compareAndSet(expectedVersion, update.getVersion())) {
                throw new ConcurrentInstrumentationException("Expected registry version: " + version.get() + ". Actual registry version: " + update.getVersion());
            }
            project.setVersion(update.getVersion());

            final CoverageDataProvider dataProvider = project.getDataProvider();
            int projLen = project.getDataLength();
            for (FullPackageInfo updatedPkgInfo : update.getChangedPkgInfos()) {
                //Look up an existing package in the model, if one exists
                final FullPackageInfo pkgInfo = (FullPackageInfo)project.getNamedPackage(updatedPkgInfo.getName());
                if (pkgInfo == null) {
                    updatedPkgInfo.setDataProvider(dataProvider);
                    project.addPackage(updatedPkgInfo);
                } else {
                    for (FullFileInfo fileInfo : (List<FullFileInfo>)updatedPkgInfo.getFiles()) {
                        fileInfo.setContainingPackage(pkgInfo);
                        fileInfo.setDataProvider(dataProvider);
                        pkgInfo.addFile(fileInfo);
                    }
                    //Extend the length of the package if instrumentation added files to this package
                    pkgInfo.setDataLength(
                        updatedPkgInfo.getDataIndex() + updatedPkgInfo.getDataLength() - pkgInfo.getDataIndex());
                    pkgInfo.invalidateCaches();
                }
                //Extend the length of the project as necessary
                projLen =
//                    Math.max(
//                        projLen,
//                        updatedPkgInfo.getDataIndex() + updatedPkgInfo.getDataLength());
                    Math.max(projLen, update.getSlotCount());
            }

            project.setDataLength(projLen);
            project.invalidateCaches();

            for (Filtered projection : filteredViews) {
                projection.applyUpdate(expectedVersion, update);
            }

            return update;
        }

        public long getVersion() {
            return version.get();
        }

        public void setVersion(long version) {
            this.version.set(version);
            project.setVersion(version);
            for (Filtered filteredView : filteredViews) {
                filteredView.setVersion(version);
            }
        }

        @Override
        public void resolve(Path sourcePath) {
            project.resolve(sourcePath);
        }
    }

    class Filtered implements ProjectView {
        private final HasMetricsFilter.Invertable filter;
        private final FullProjectInfo project;

        public Filtered(HasMetricsFilter.Invertable filter, FullProjectInfo orig) {
            this.filter = filter;
            this.project = orig.copy(filter);
        }

        @Override
        public FullProjectInfo getProject() {
            return project;
        }

        @Override
        @SuppressWarnings("unchecked")
        public RegistryUpdate applyUpdate(long expectedVersion, InstrumentationSessionImpl.Update update) {
            project.setVersion(update.getVersion());

            final CoverageDataProvider dataProvider = project.getDataProvider();
            int projLen = project.getDataLength();
            for (FullPackageInfo updatedPkgInfo : update.getChangedPkgInfos()) {
                //Look up an existing package in the model, if one exists
                FullPackageInfo pkgInfo = (FullPackageInfo)project.getNamedPackage(updatedPkgInfo.getName());

                for (FullFileInfo fileInfo : (List<FullFileInfo>)updatedPkgInfo.getFiles()) {
                    if (filter.accept(fileInfo)) {
                        //Create a new filtered packageInfo but only on demand
                        if (pkgInfo == null) {
                            pkgInfo = new FullPackageInfo(project, updatedPkgInfo.getName(), updatedPkgInfo.getDataIndex());
                            pkgInfo.setDataProvider(dataProvider);
                            project.addPackage(pkgInfo);
                        }

                        FullFileInfo fileInfoCopy = fileInfo.copy(pkgInfo, filter);
                        fileInfoCopy.setDataProvider(dataProvider);
                        fileInfoCopy.setContainingPackage(pkgInfo);
                        pkgInfo.addFile(fileInfoCopy);
                    }
                }

                if (pkgInfo != null) {
                    //Extend the length of the package if instrumentation added files to this package
                    pkgInfo.setDataLength(
                        updatedPkgInfo.getDataIndex() + updatedPkgInfo.getDataLength() - pkgInfo.getDataIndex());
                    pkgInfo.invalidateCaches();
                }

                //Extend the length of the project, even if the package was filtered away
                projLen =
//                    Math.max(
//                        projLen,
//                        updatedPkgInfo.getDataIndex() + updatedPkgInfo.getDataLength());
                    Math.max(projLen, update.getSlotCount());
            }

            project.setDataLength(projLen);
            project.invalidateCaches();

            return update;
        }

        private void setVersion(long version) {
            project.setVersion(version);
        }

        @Override
        public void resolve(Path sourcePath) {
            project.resolve(sourcePath);
        }
    }
}
