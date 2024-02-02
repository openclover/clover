package org.openclover.idea.build.jps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.incremental.BuilderService;
import org.jetbrains.jps.incremental.ModuleLevelBuilder;

import java.util.List;

import static org.openclover.util.Lists.newArrayList;

/**
 * Service which returns an instance of CloverJavaBuilder as an additional builder for a module.
 */
@SuppressWarnings("Unused") // see META-INF\services
public class CloverBuilderService extends BuilderService {
    @NotNull
    @Override
    public List<? extends ModuleLevelBuilder> createModuleLevelBuilders() {
        final List<ModuleLevelBuilder> moduleLevelBuilders = newArrayList();
        moduleLevelBuilders.add(CloverJavaBuilder.getInstance());
        return moduleLevelBuilders;
    }
}
