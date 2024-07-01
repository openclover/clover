package org.openclover.idea.report.treemap;

import net.sf.jtreemap.swing.DefaultToolTip;
import net.sf.jtreemap.swing.DefaultValue;
import net.sf.jtreemap.swing.IToolTipBuilder;
import net.sf.jtreemap.swing.JTreeMap;
import net.sf.jtreemap.swing.TreeMapNode;
import net.sf.jtreemap.swing.TreeMapNodeBuilder;
import net.sf.jtreemap.swing.Value;
import net.sf.jtreemap.swing.ValuePercent;
import net.sf.jtreemap.swing.provider.HSBTreeMapColorProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.openclover.core.CloverDatabase;
import org.openclover.idea.coverage.CoverageListener;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.util.vfs.DummyFileEditor;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import java.awt.Color;

import static net.sf.jtreemap.swing.provider.HSBTreeMapColorProvider.ColorDistributionTypes.SQUARE_ROOT;

public class TreeMapEditor extends DummyFileEditor implements CoverageListener {
    private final JTreeMap treeMap;
    private final CoverageManager coverageManager;

    public TreeMapEditor(Project project, TreeMapVirtualFile virtualFile) {
        coverageManager = virtualFile.getCoverageManager();
        final TreeMapNode root = new ProjectHeapMapBuilder().buildTree(coverageManager.getCoverage());
        treeMap = new JTreeMap(root);
        treeMap.setToolTipBuilder(new ToolTipBuilder(treeMap));
        treeMap.setColorProvider(new IdeaCoverageColorProvider());

        treeMap.addMouseListener(new GoToSourceMouseListener(project, treeMap));

        coverageManager.addCoverageListener(this);
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return treeMap;
    }

    @Override
    @NotNull
    public String getName() {
        return "CloverTreeMapEditor";
    }

    @Override
    public void update(CloverDatabase db) {
        TreeMapNode tmpRoot;
        if (db != null) {
            // build treemap using information from coverage database
            tmpRoot = new ProjectHeapMapBuilder().buildTree(db);
            // ... but in case when tree is empty (i.e. no code in project) use empty one
            if (tmpRoot.getChildren() == null || tmpRoot.getChildren().isEmpty()) {
                tmpRoot = buildCoverageUnavailableTree();
            }
        } else {
            // case when database is unavailable (i.e. clover is disabled for project)
            tmpRoot = buildCoverageUnavailableTree();
        }

        // need final variable for inner Runnable class
        final TreeMapNode newRoot = tmpRoot;
        ApplicationManager.getApplication().invokeLater(() -> {
            treeMap.setRoot(newRoot);
            treeMap.setActiveLeaf(null);
            treeMap.setColorProvider(new IdeaCoverageColorProvider());
            treeMap.repaint();
        });
    }

    @Override
    public void dispose() {
        coverageManager.removeCoverageListener(this);
    }

    /**
     * Creates a tree with a single leaf containing message that that coverage is not available.
     * @return TreeMapNode
     */
    protected TreeMapNode buildCoverageUnavailableTree() {
        final TreeMapNodeBuilder builder = new TreeMapNodeBuilder();
        final TreeMapNode newRoot = builder.buildBranch("", null);
        final TreeMapNode top = builder.buildBranch("Coverage information unavailable", newRoot);
        builder.buildLeaf("Coverage information unavailable", 1, new ValuePercent(0), top);
        return newRoot;
    }
}

class ToolTipBuilder implements IToolTipBuilder {
    private final JToolTip instance;

    public ToolTipBuilder(JTreeMap treeMap) {
        this.instance = new DefaultToolTip(treeMap, null, null, false);
    }


    @Override
    public JToolTip getToolTip() {
        return instance;
    }
}

class HSBColorProvider extends HSBTreeMapColorProvider {
    HSBColorProvider(JTreeMap treeMap) {
        super(treeMap, SQUARE_ROOT, Color.GREEN, Color.RED);
    }

    /**
     * Rescales the 0:100 range to -50 : 50.
     *
     * @see net.sf.jtreemap.swing.provider.ColorProvider#getColor(net.sf.jtreemap.swing.Value)
     */
    @Override
    public Color getColor(Value value) {
        final Value mappedValue = new DefaultValue(value.getValue() - 50d);
        return super.getColor(mappedValue);
    }
}

