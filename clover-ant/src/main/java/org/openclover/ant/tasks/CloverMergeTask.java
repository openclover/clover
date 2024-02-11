package org.openclover.ant.tasks;

import org.openclover.core.CloverDatabase;
import org.openclover.core.CloverDatabaseSpec;
import org.openclover.core.cfg.Interval;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileSet;

import java.util.List;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Lists.newLinkedList;

public class CloverMergeTask extends AbstractCloverTask {


    private List<CloverDatabaseSpec> cloverDbs = newArrayList();
    private List<CloverDbSet> cloverDbSets;

    private boolean update = false;

    private Interval updateSpan = Interval.DEFAULT_SPAN;


    public static class CloverDbSet extends FileSet {

        private Interval span = Interval.DEFAULT_SPAN;

        public CloverDbSet() {
           super();
        }

        public Interval getSpan() {
            return span;
        }

        public void setSpan(Interval span) {
            this.span = span;
        }

        public List<CloverDatabaseSpec> getIncludedDbs() {

            final FileSet fs = (isReference()) ? getCheckedRef(FileSet.class, "fileset") : this;
            final String [] files = fs.getDirectoryScanner(getProject()).getIncludedFiles();
            final List<CloverDatabaseSpec> dbs = newLinkedList();
            final String baseDir = fs.getDir(getProject()).getAbsolutePath();

            for (final String fileName : files) {
                dbs.add(new CloverDatabaseSpec(baseDir + "/" + fileName, span));
            }

            return dbs;
        }
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void setUpdateSpan(Interval updateSpan) {
        this.updateSpan = updateSpan;
    }

    public void addCloverDb(CloverDatabaseSpec db) {
        cloverDbs.add(db);
    }

    public void addCloverDbSet(CloverDbSet dbset) {
        if (cloverDbSets == null) {
            cloverDbSets = newArrayList();
        }

        cloverDbSets.add(dbset);
    }

    @Override
    public void cloverExecute() {

        if (getInitString() == null) {
            throw new BuildException("You must specify the location "
                    + "of the new clover database with the \"initString\" "
                    + "attribute");            
        }
        String initString = resolveInitString();

        if (cloverDbSets != null) {
          for (final CloverDbSet dbset : cloverDbSets) {
              cloverDbs.addAll(dbset.getIncludedDbs());
          }
        }

        if (cloverDbs.size() == 0 && !update) {
            throw new BuildException("You must specify one or more" +
                    " coverage databases to merge using a nested " +
                    " <cloverdb> or <cloverdbset> element.");
        }


        try {
            CloverDatabase.merge(cloverDbs, initString, update, updateSpan, (desc, pc) -> log(desc));
        }
        catch (Exception e) {
            throw new BuildException("Error writing new clover db at \"" + initString + "\": " + e.getMessage(), e);
        }
    }
}
