package com.atlassian.clover.model;

import org.openclover.runtime.api.CloverException;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.registry.entities.BaseFileInfo;
import com.atlassian.clover.registry.entities.BasePackageInfo;
import com.atlassian.clover.registry.entities.FullClassInfo;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.registry.entities.FullProjectInfo;
import com.atlassian.clover.registry.entities.Modifiers;
import com.atlassian.clover.registry.metrics.BlockMetrics;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.metrics.ProjectMetrics;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.GZIPInputStream;

/**
 * converts an XML document produced by the XMLReporter into a Clover coverage model.
 */
public class XmlConverter {
    public final static int TOP_LEVEL = 0;
    public final static int PROJECT_LEVEL = 1;
    public final static int PACKAGE_LEVEL = 2;
    public final static int FILE_LEVEL = 3;
    public final static int CLASS_LEVEL = 4;
    public final static int LINE_LEVEL = 5;

    public static class CoverageXMLHandler extends DefaultHandler {

        private CoverageDataPoint model;
        private FullProjectInfo project;
        private FullPackageInfo pkg;
        private FullFileInfo file;
        private HasMetrics currentEntity;

        private int currentLevel = TOP_LEVEL;
        private int requiredLevel = PACKAGE_LEVEL;
        private boolean seenProjectElement = false; // hack - quick hack to make history points that contain testproject elements load correctly. They are ignored for the moment.

        public CoverageXMLHandler(int detailLevel) {
            requiredLevel = detailLevel;
        }

        @Override
        public void startDocument() {
            model = new CoverageDataPoint();
            seenProjectElement = false;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {

            if (seenProjectElement) {
                return;
            }
            switch (qName) {
                case XmlNames.E_COVERAGE:
                    model.setVersion(getAttribute(atts, XmlNames.A_CLOVER, "????"));
                    model.setGenerated(Long.parseLong(atts.getValue(XmlNames.A_GENERATED)));
                    break;
                case XmlNames.E_PROJECT:
                    currentLevel = PROJECT_LEVEL;
                    if (requiredLevel >= currentLevel) {
                        project = new FullProjectInfo(
                                atts.getValue(XmlNames.A_NAME, ""),
                                Long.parseLong(atts.getValue(XmlNames.A_TIMESTAMP)));
                        model.setProject(project);
                        currentEntity = project;
                    }
                    break;
                case XmlNames.E_PACKAGE:
                    currentLevel = PACKAGE_LEVEL;
                    if (requiredLevel >= currentLevel) {
                        pkg = new FullPackageInfo(project, atts.getValue(XmlNames.A_NAME), 0);
                        project.addPackage(pkg);
                        currentEntity = pkg;
                    }
                    break;
                case XmlNames.E_FILE:
                    currentLevel = FILE_LEVEL;
                    if (requiredLevel >= currentLevel) {
                        file = new FullFileInfo(
                                pkg,
                                new File(atts.getValue(XmlNames.A_NAME)),
                                atts.getValue(XmlNames.A_ENCODING),
                                0, 0, 0, 0, 0, 0, 0); //hack - old historypoints don't have these values, so leave em as zero for the moment. new ones should
                        pkg.addFile(file);
                        currentEntity = file;
                    }
                    break;
                case XmlNames.E_CLASS:
                    currentLevel = CLASS_LEVEL;
                    if (requiredLevel >= currentLevel) {
                        FullClassInfo clazz = new FullClassInfo(
                                pkg, file, 0, atts.getValue(XmlNames.A_NAME),
                                new FixedSourceRegion(0, 0, 0, 0)/* TODO */,
                                new Modifiers(),
                                false, false, false); //hack - old historypoints don't have these values, so leave em as zero for the moment. new ones should
                        file.addClass(clazz);
                        currentEntity = clazz;
                    }
                    break;
                case XmlNames.E_METRICS:
                    BlockMetrics mets = getMetrics(atts, currentEntity);
                    if (requiredLevel >= currentLevel) {
                        currentEntity.setMetrics(mets);
                    }
                    break;
                default:
                    // ignore
                    break;
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            switch (qName) {
                case XmlNames.E_PROJECT:
                    currentLevel = TOP_LEVEL;
                    seenProjectElement = true;
                    break;
                case XmlNames.E_PACKAGE:
                    currentLevel = PROJECT_LEVEL;
                    if (currentEntity instanceof BasePackageInfo) {
                        currentEntity = ((BasePackageInfo) currentEntity).getContainingProject();
                    }
                    break;
                case XmlNames.E_FILE:
                    currentLevel = PACKAGE_LEVEL;
                    if (currentEntity instanceof BaseFileInfo) {
                        currentEntity = ((BaseFileInfo) currentEntity).getContainingPackage();
                    }
                    break;
                case XmlNames.E_CLASS:
                    currentLevel = FILE_LEVEL;
                    if (currentEntity instanceof BaseClassInfo) {
                        currentEntity = ((BaseClassInfo) currentEntity).getContainingFile();
                    }
                    break;
            }
        }

        @Override
        public void endDocument() {
        }

        private ProjectMetrics getMetrics(Attributes atts, HasMetrics owner) {
            ProjectMetrics mets = new ProjectMetrics(owner);
            mets.setNumMethods(Integer.parseInt(atts.getValue(XmlNames.A_METHODS)));
            mets.setNumStatements(Integer.parseInt(atts.getValue(XmlNames.A_STATEMENTS)));
            mets.setNumBranches(Integer.parseInt(atts.getValue(XmlNames.A_CONDITIONALS)));
            mets.setNumCoveredMethods(Integer.parseInt(atts.getValue(XmlNames.A_COVEREDMETHODS)));
            mets.setNumCoveredStatements(Integer.parseInt(atts.getValue(XmlNames.A_COVEREDSTATEMENTS)));
            mets.setNumCoveredBranches(Integer.parseInt(atts.getValue(XmlNames.A_COVEREDCONDITIONALS)));
            final String complexity = atts.getValue(XmlNames.A_COMPLEXITY);
            if (complexity != null) {
                mets.setComplexity(Integer.parseInt(complexity));
            }
            
            String val = atts.getValue(XmlNames.A_PACKAGES);
            if (val != null) {
                mets.setNumPackages(Integer.parseInt(val));
            }

            val = atts.getValue(XmlNames.A_FILES);
            if (val != null) {
                mets.setNumFiles(Integer.parseInt(val));
            }

            val = atts.getValue(XmlNames.A_CLASSES);
            if (val != null) {
                mets.setNumClasses(Integer.parseInt(val));
            }

            val = atts.getValue(XmlNames.A_LOC);
            if (val != null) {
                mets.setLineCount(Integer.parseInt(val));
            }

            val = atts.getValue(XmlNames.A_NCLOC);
            if (val != null) {
                mets.setNcLineCount(Integer.parseInt(val));
            }

            return mets;
        }

        private static String getAttribute(Attributes atts, String attributeName,
                                           String defaultValue) {
            String value = atts.getValue(attributeName);
            if (value == null) {
                value = defaultValue;
            }

            return value;
        }

        public CoverageDataPoint getDataPoint() {
            return model;
        }
    }

    public static CoverageDataPoint getFromXmlFile(File in, int detailLevel)
            throws IOException, CloverException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            CoverageXMLHandler handler = new CoverageXMLHandler(detailLevel);
            parser.parse(getInputStream(in), handler);
            CoverageDataPoint result = handler.getDataPoint();
            result.setDataFile(in);
            return handler.getDataPoint();
        } catch (ParserConfigurationException | SAXException e) {
            throw new CloverException(e);
        }
    }

    private static InputStream getInputStream(File inf) throws IOException {
        InputStream in = new BufferedInputStream(Files.newInputStream(inf.toPath()));
        if (inf.getName().endsWith(".gz")) {
            return new GZIPInputStream(in);
        } else {
            return in;
        }
    }
}
