package org.openclover.idea.coverage;

import org.jetbrains.annotations.Nullable;
import org.openclover.core.CloverDatabase;

/**
 * The CoverageListener interface provides a callback for when a CloverDatabase
 * changes in some way.
 */
public interface CoverageListener {

    /**
     * A coverage model update notification.
     *
     * @param db newly read CloverDatabase - may be null
     */
    void update(@Nullable CloverDatabase db);
}
