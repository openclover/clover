package org.openclover.ci;

import java.io.File;
import java.io.Serializable;

public class CIOptions implements Serializable {

    private final boolean optimize;
    private final boolean html;
    private final boolean json;
    private final boolean pdf;
    private final boolean xml;

    private final boolean historical;
    private final File historyDir;
    private final boolean fullClean;
    private final boolean putValuesInQuotes;

    private CIOptions(Builder builder) {
        this.optimize = builder.optimize;
        this.html = builder.html;
        this.json = builder.json;
        this.historical = builder.historical;
        this.historyDir = builder.historyDir;
        this.fullClean = builder.fullClean;
        this.pdf = builder.pdf;
        this.xml = builder.xml;
        this.putValuesInQuotes = builder.putValuesInQuotes;
    }

    public boolean isOptimize() {
        return optimize;
    }

    public boolean isHtml() {
        return html;
    }

    public boolean isJson() {
        return json;
    }

    public File getHistoryDir() {
        return historyDir;
    }

    public boolean isHistorical() {
        return historical;
    }

    public boolean isFullClean() {
        return fullClean;
    }

    public boolean isPdf() {
        return pdf;
    }

    public boolean isXml() {
        return xml;
    }

    public boolean isPutValuesInQuotes() {
        return putValuesInQuotes;
    }

    /**
     * This class is used to configure one of the {@link Integrator} classes.
     * Obtain a set of CIOptions using the static factory method: CIOptions.newDefaults().
     */
    public static class Builder implements Serializable {

        private boolean optimize = false;
        private boolean html = true;
        private boolean json = false;
        private boolean pdf = false;
        private boolean xml = true;
        private boolean historical = false;
        private File historyDir = null;
        private boolean fullClean = false;
        private boolean putValuesInQuotes = false;

        /**
         * Creates a brand new CIOptionsBuilder with default values.
         */
        public Builder() {

        }

        /**
         * Whether or not to optimize tests.
         *
         * @param optimize true if tests should be optimized using Clover's test optimization
         * @return these options, but with optimization turned on.
         */
        public Builder optimize(boolean optimize) {
            this.optimize = optimize;
            return this;
        }

        public Builder html(boolean html) {
            this.html = html;
            return this;
        }

        public Builder json(boolean json) {
            this.json = json;
            return this;
        }

        public Builder pdf(boolean pdf) {
            this.pdf = pdf;
            return this;
        }

        public Builder xml(boolean xml) {
            this.xml = xml;
            return this;
        }

        public Builder historical(boolean historical) {
            this.historical = historical;
            return this;
        }

        /**
         * Use the given history directory to store Clover artifacts which are needed by Clover between builds.
         * For example: Clover history points used to generate a Clover Historical Report,
         * or the clover.snapshot file needed for Test Optimization.
         *
         * @param historyDir a directory to store Clover data between builds.
         * @return Builder this
         */
        public Builder historyDir(File historyDir) {
            this.historyDir = historyDir;
            return this;
        }

        /**
         * @param fullClean true if all previous clover data (excluding the historydir) should be removed before running
         *                  the build.
         * @return Builder this
         */
        public Builder fullClean(boolean fullClean) {
            this.fullClean = fullClean;
            return this;
        }

        /**
         * <p>Applicable for AntIntegrator only.</p>
         *
         * <p>If set to <code>true</code> then some of Clover's properties will be put in double quotes. This is to
         * solve a problem with properties containing whitespace characters. Ant prior to version 1.9.7 passes
         * arguments to the JVM process incorrectly on non-Windows operating systems - for example "[-Da=b c][-De=f]"
         * may be passed as "[-Da=b] [c] [-De=f]".</p>
         *
         * <p>Set this value to <code>true</code> if the following conditions are met:
         * <ul>
         *
         *  <li>you have Ant version older than 1.9.7</li>
         *  <li>you are not building on Windows platform</li>
         * </ul>
         *
         * <p>Double quotes will not be added on Windows platform, even if the property is set to <code>true</code>.</p>
         *
         * <p>Ant since version 1.9.7 passes arguments with spaces correctly therefore do not set this parameter to
         * <code>true</code>, otherwise you'll end up with values containing double quotes.</p>
         *
         * @param inQuotes <code>true</code> - put values in double quotes, <code>false</code> - put values as is
         * @return Builder this
         */
        public Builder putValuesInQuotes(boolean inQuotes) {
            this.putValuesInQuotes = inQuotes;
            return this;
        }

        public CIOptions build() {
            return new CIOptions(this);
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "optimize=" + optimize +
                    ", html=" + html +
                    ", json=" + json +
                    ", pdf=" + pdf +
                    ", xml=" + xml +
                    ", historical=" + historical +
                    ", historyDir='" + historyDir + "'" +
                    ", fullClean=" + fullClean +
                    ", putValuesInQuotes=" + putValuesInQuotes +
                    '}';
        }
    }

}