package org.openclover.core.reporters.html;

import clover.org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.CoverageData;
import org.openclover.core.api.registry.BlockMetrics;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.registry.CoverageDataRange;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.entities.BaseFileInfo;
import org.openclover.core.registry.entities.BasePackageInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.FullMethodInfo;
import org.openclover.core.registry.entities.FullProjectInfo;
import org.openclover.core.registry.entities.StackTraceInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.registry.util.EntityVisitorUtils;
import org.openclover.core.reporters.Column;
import org.openclover.core.reporters.Format;
import org.openclover.core.reporters.util.ReportColors;
import org.openclover.core.spi.reporters.html.source.HtmlRenderingSupport;
import org.openclover.core.util.FileUtils;
import org.openclover.runtime.Logger;
import org.openclover.runtime.api.CloverException;
import org.openclover.runtime.util.Formatting;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openclover.core.util.Maps.newHashMap;


/**
 * Now that rendering of velocity macros is performed from multiple threads, methods contained in this class
 * should be thread safe.
 */
public class HtmlRenderingSupportImpl implements HtmlRenderingSupport {
    // NB: this object is accessed from multiple threads and must therefore be read only.
    private final Format format;
    private final boolean filter;
    private static final String FILE_REGEXP = "[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*\\.java:([0-9]+)";
    private static final Pattern TRACE_LINE_PATTERN = Pattern.compile("((([\\p{Alpha}\\$_][\\p{Alnum}\\$_]*\\.)*[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*)\\.(?:&lt;)?[\\p{Alpha}\\$_][\\p{Alnum}\\$_]*(?:&gt;)?)\\((" + FILE_REGEXP + "|Unknown Source)\\)");
    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile(FILE_REGEXP);

    public HtmlRenderingSupportImpl() {
        this(Format.DEFAULT_HTML, true);
    }

    public HtmlRenderingSupportImpl(Format format, boolean filter) {
        this.format = format;
        this.filter = filter;
    }

    public Map<String, Number> collectColumnValues(List<Column> columns, HasMetrics info) throws CloverException {
        Map<String, Number> columnValues = newHashMap();

        for (Column col : columns) {
            BlockMetrics metrics = metricsFor(info);
            col.init(metrics);
            columnValues.put(col.getName(), col.getNumber());
            col.reset();
        }
        return columnValues;
    }

    public BlockMetrics metricsFor(HasMetrics info) {
        return !filter ? info.getRawMetrics() : info.getMetrics();
    }

    public String formatShortDate(long ts) {
        return Formatting.formatShortDate(new Date(ts));
    }

    public String getPercentStr(float aPc) {
        return Formatting.getPercentStr(aPc);
    }

    public String getRemainderPercentStr(float aPc) {
        float remainder = getRemainder(aPc);
        return getPercentStr(remainder);
    }

    private float getRemainder(float aPc) {
        return aPc < 0 ? -1 : 1.0f - aPc;
    }

    public String getPkgURLPath(String aPkg) {
        return aPkg.replace('.', '/') + "/";
    }

    public String getPkgURLPath(String aPkg, String aFile) {
        return aPkg.replace('.', '/') + "/" + aFile;
    }
    
    public String getFileIdentifier(FullFileInfo aFile) {
        return aFile.getPackagePath().replace('/', '_').replace('.', '_');
    }
    
    @Override
    public String getRootRelPath(String aPkg)
    {
        final int l = aPkg.length();
        if (l == 0) {
            return "";
        }
        final StringBuilder buf = new StringBuilder(l);
        buf.append("../");
        for (int i = 0; i < l; i++) {
            if (aPkg.charAt(i) == '.') {
                buf.append("../");
            }
        }
        return buf.toString();
    }

    /**
     * @return the path of package pkgA relative to the pkgB, ie: we are in pkgB
     * and want to get to package A.
     */
    @Override
    public String getPackageRelPath(String pkgA, String pkgB) {

        if (pkgA == null || pkgA.compareTo("") == 0) {
            return getRootRelPath(pkgB);
        }

        if (pkgB == null || pkgB.compareTo("") == 0) {
            return getPkgURLPath(pkgA);
        }

        StringTokenizer pkgATokens = new StringTokenizer(pkgA, ".");
        StringTokenizer pkgBTokens = new StringTokenizer(pkgB, ".");

        String tokenA = null;
        String tokenB = null;
        while (pkgATokens.hasMoreTokens() && pkgBTokens.hasMoreTokens()) {
            tokenA = pkgATokens.nextToken();
            tokenB = pkgBTokens.nextToken();
            if (tokenA.compareTo(tokenB) != 0) {
                break;
            }
        }

        final StringBuilder buf = new StringBuilder();
        if (tokenB != null) {
            buf.append("../");
        }
        while (pkgBTokens.hasMoreTokens()) {
            pkgBTokens.nextToken();
            buf.append("../");
        }

        if (tokenA != null) {
            buf.append(tokenA).append("/");
        }
        while (pkgATokens.hasMoreTokens()) {
            buf.append(pkgATokens.nextToken()).append("/");
        }

        return buf.toString();
    }

    // IE gives grief rendering long strings (like file paths) inside tables, so this inserts
    // a few spaces in the path. another option is to insert a soft hyphen (&shy;) but neither
    // are ideal, because the string won't cut & paste.
    public String getWrappablePath(String aStr)
    {
        int l = aStr.length();
        StringBuilder buf = new StringBuilder(l);
        for (int i = 0; i < l; i++) {
            if (aStr.charAt(i) == File.separatorChar) {
                buf.append(File.separatorChar).append(" ");
            }
            else {
                buf.append(aStr.charAt(i));
            }
        }
        return buf.toString();
    }

    public StringBuffer getTestFileName(TestCaseInfo testInfo) {
        final String className = testInfo.getRuntimeType().getName();

        final StringBuffer link = new StringBuffer();
        final String testIdBase36 = Integer.toString(testInfo.getId(), 36);
        // file name limit is 255 characters, so shorten class name and test name if necessary
        final String classNameSanitized = shortenName(className.replaceAll("\\W", "_"),
                254 - testIdBase36.length() - "__.html".length());
        final String testNameSanitized = shortenName(testInfo.getTestName().replaceAll("\\W", "_"),
                254 - testIdBase36.length() - classNameSanitized.length() - "__.html".length());

        link.append(classNameSanitized)
                .append("_")
                .append(testNameSanitized)
                .append("_")
                .append(testIdBase36)
                .append(".html");
        return link;
    }

    static String shortenName(String originalName, int maxCharacters) {
        // return original name if fits in the limit
        if (originalName == null || originalName.length() <= maxCharacters) {
            return originalName;
        }

        // get non-negative hash from name
        final int absHash = originalName.hashCode() != Integer.MIN_VALUE ?
                Math.abs(originalName.hashCode()) :
                Integer.MAX_VALUE;
        final String hashBase36 = Integer.toString(absHash, 36);
        final int originalLength = maxCharacters - hashBase36.length();

        // return only hash, truncate it if needed
        if (originalLength <= 0) {
            return hashBase36.substring(0, Math.min(hashBase36.length(), Math.max(0, maxCharacters)));
        }

        // return truncated name + full hash
        return originalName.substring(0, originalLength) + hashBase36;
    }

    public StringBuffer getTestLink(boolean topLevel, TestCaseInfo testInfo) {
        final StringBuffer link = new StringBuffer();

        if (testInfo == null || !testInfo.isResolved()) {
            Logger.getInstance().debug("No test information found" + (testInfo == null? "." : " for " + testInfo.getRuntimeTypeName() + "." + testInfo.getSourceMethodName()));
            return link; // return an empty link. see CCD-444. 
        }

        if (topLevel) {
            final String pkgURLPath = getPkgURLPath(testInfo.getRuntimeType().getPackage().getName());
            link.append(pkgURLPath);
        }
        link.append(getTestFileName(testInfo));
        return link;
    }

    public StringBuffer getSrcFileLink(boolean toplevel, BaseClassInfo cls, long idParam) {
        final String cname = cls.getName();
        final FileInfo fileInfo = cls.getContainingFile();
        final String file = fileInfo != null ? fileInfo.getName() : "";
        final String pkgName = cls.getPackage().getName();
        final StringBuffer srcFileLink = getSrcFileLink(toplevel, true, cname, file, pkgName);

        return insertQueryParams(srcFileLink, idParam);
    }

    private StringBuffer insertQueryParams(StringBuffer srcFileLink, long idParam) {
        final StringBuffer retVal = new StringBuffer();
        final int hashIndex = srcFileLink.lastIndexOf("#");
        final String paramString = "?id=" + idParam;
        if (hashIndex >= 0) {
            retVal.append(srcFileLink.substring(0, hashIndex));
            retVal.append(paramString);
            retVal.append(srcFileLink.substring(hashIndex, srcFileLink.length()));
        } else {
            retVal.append(srcFileLink).append(paramString);
        }
        return retVal;
    }


    public StringBuffer getFileLink(boolean toplevel, BaseFileInfo fileInfo) {
        final String file = fileInfo.getName();
        final String pkgName = fileInfo.getContainingPackage().getName();
        return getSrcFileLink(toplevel, false, file, file, pkgName);
    }

    public StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, BaseClassInfo cls, BasePackageInfo pkgInContext) {
        final String cname = cls.getName();
        final FileInfo fileInfo = cls.getContainingFile();
        final String file = fileInfo != null ? fileInfo.getName() : "";
        final String pkgName = cls.getPackage().getName();
        final String pkgNameInContext = pkgInContext.getName();

        StringBuffer link = new StringBuffer();
        link.append(getPackageRelPath(pkgName, pkgNameInContext));
        appendBaseFileName(link, file);
        if (withAnchor) {
            link.append("#").append(cname);
        }

        return link;
    }

    @Override
    public StringBuffer getSrcFileLink(boolean toplevel, boolean withAnchor, ClassInfo cls) {
        final String cname = cls.getName();
        final FileInfo fileInfo = cls.getContainingFile();
        final String file = fileInfo != null ? fileInfo.getName() : "";
        final String pkgName = cls.getPackage().getName();

        return getSrcFileLink(toplevel, withAnchor, cname, file, pkgName);
    }

    public String getMethodLink(boolean toplevel, FullMethodInfo mthd) {
        if (mthd == null) {  //hack - see CCD-303 - cope with null methods
            return "";
        }
        return getSrcLineLink(toplevel, mthd.getContainingFile(), mthd.getStartLine());
    }

    public String getSrcLineLink(boolean toplevel, FileInfo file, int line) {
        final StringBuffer link = new StringBuffer();
        appendBaseDirectoryName(link, toplevel, file.getContainingPackage().getName());
        appendBaseFileName(link, file.getName());
        if (line != -1) {
            link.append("?line=").append(line).append("#src-").append(line);
        }

        return link.toString();
    }

    public StringBuffer getSrcFileLink(boolean isTopLevel, boolean withAnchor, String className, String containingFileName, String packageName) {
        StringBuffer link = new StringBuffer();
        String basename = appendBaseDirectoryName(link, isTopLevel, packageName);
        appendBaseFileName(link, containingFileName);
        if (withAnchor) {
            link.append("#").append(className);
        }
        return link;
    }

    public String getMethodIndentiation(final MethodInfo methodInfo) {
        final EntityVisitorUtils utils = new EntityVisitorUtils();
        StringBuilder indent = new StringBuilder();
        MethodInfo current = methodInfo;
        while ( (current = utils.GET_PARENT.asMethod(current)) != null ) {
            indent.append("&#160;&#160;&#160;&#160;");
        }
        return indent.toString();
    }

    private String appendBaseDirectoryName(StringBuffer link, boolean isTopLevel, String packageName) {
        final String pkgUrl = isTopLevel ? getPkgURLPath(packageName) : "";
        link.append(pkgUrl);
        return pkgUrl;
    }

    private void appendBaseFileName(StringBuffer link, String containingFileName) {
        String basename = containingFileName.substring(0, containingFileName.lastIndexOf("."));
        link.append(basename).append(".html");
    }

    public String getBaseFileName(BaseClassInfo classInfo) {
        StringBuffer buf = new StringBuffer();
        appendBaseDirectoryName(buf, true, classInfo.getPackage().getName());

        final String normalizedPath = FileUtils.getNormalizedPath(classInfo.getContainingFile().getName());
        String fileName = normalizedPath;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }

        appendBaseFileName(buf, fileName);

        return buf.toString();
    }

    public static int getWidth(int w, float aPc)
    {
        return (int)(((float)w)*Math.abs(aPc));
    }

    public static int getRemainder(int w, float aPc)
    {
        return w - getWidth(w,aPc);
    }

    public static String getPcWidth(float pc) {
        return Formatting.getPcWidth(100*pc);
    }
    
    public static String getPcWidth(Float pc) {
        return getPcWidth(pc.floatValue());
    }

    public String getRemainderPcWidth(float pc) {
        float remainder = getRemainder(pc);
        if (remainder < 0) {
            return "0";
        }
        return Formatting.getPcWidth(100 * remainder);
    }

    public String format1d(float pc)
    {
        return formatMultiply1d(pc, 100);
    }

    public String formatMultiply1d(float pc, float multiplier) {
        return Formatting.format1d(pc * multiplier);
    }

    public String formatMultiply1dReverse(float pc, float multiplier, float max) {
        return formatMultiply1d(max - pc, multiplier);
    }

    public String truncateEnd(String str) {
        return htmlEscapeStr(Formatting.restrictLength(str, format.getMaxNameLength(), false));
    }
    public String truncateEnd(String str, int length) {
        return htmlEscapeStr(Formatting.restrictLength(str, length, false));
    }

    public String truncateStart(String str) {
        return htmlEscapeStr(Formatting.restrictLength(str, format.getMaxNameLength(), true));
    }

    //##HACK - remove this adapter method and use Formatting directly
    public String formatInt(int aVal)
    {
        return Formatting.formatInt(aVal);
    }

    public String format2d(double aVal)
    {
        return Formatting.format2d(aVal);
    }

    public String format3d(double aVal)
    {
        return Formatting.format3d(aVal);
    }


    public String capitalize(String s) {
        if (s != null) {
            if (s.length() > 1) {
                return s.substring(0,1).toUpperCase(Locale.ENGLISH) + s.substring(1);
            }
            return s.toUpperCase(Locale.ENGLISH);
        }
        return s;
    }

    /**
     * Join two strings. Add a dot between them if they're not empty. Function is useful for contatenating
     * a package name.
     */
    public String joinWithDots(String s1, String s2) {
        return s1 + (s1.isEmpty() || s2.isEmpty() ? "" : ".") + s2;
    }

    /**
     * Check if obj in an ArrayList, get last element, contatenate with a key and add to the end of the list.
     */
    public void listAddKeyAsLast(@NotNull ArrayList<String> list, @NotNull String key) {
        String prefix = list.size() > 0 ?
                list.get(list.size() - 1) :
                "";
        list.add(joinWithDots(prefix, key));
    }

    /**
     * ArrayList does not have getLast() method; and we need it in one velocity template.
     */
    @Nullable
    public String listGetLast(@NotNull ArrayList<String> list) {
        return list.size() > 0 ? list.get(list.size() - 1) : null;
    }

    /**
     * ArrayList does not have removeLast() method; and we need it in one velocity template.
     */
    public void listRemoveLast(@NotNull ArrayList list) {
        if (list.size() > 0) {
            list.remove(list.size() - 1);
        }
    }

    public int contains(Set set, int key) {
        return set.contains(key) ? 1: 0;
    }

    public int length(Object[] array) {
        return array != null ? array.length : -1;
    }

    public String htmlEscapeStr(String aString) {
        return htmlEscapeStr(aString, "&#09;", "&#160;");
    }

    @Override
    public String htmlEscapeStr(String aString, String tabString, String spaceString) {
        if (aString == null) {
            return "";
        }

        StringBuilder buf = new StringBuilder(aString.length() * 3);
        for (int i = 0, j = aString.length(); i < j; i++) {
            char c = aString.charAt(i);
            switch (c) {
                case ' ':
                    buf.append(spaceString);
                    break;
                case '<':
                    buf.append("&lt;");
                    break;
                case '>':
                    buf.append("&gt;");
                    break;
                case '\t':
                    buf.append(tabString);
                    break;
                case '&':
                    buf.append("&amp;");
                    break;
                case '"':
                    buf.append("&quot;");
                    break;
                default:
                    buf.append(c);
            }
        }
        return buf.toString();
    }

    @SuppressWarnings("unused") //used by velocity, inline-dialog-help.vm
    public String javaScriptEscapeStr(String input) {
        return StringEscapeUtils.escapeJson(input);
    }

    /**
     * renders a stack trace by inserting hyperlinking for class/filenames where found.
     * @return stack trace with hyperlinked class links
     */
    public String linkifyStackTrace(String rootRelPath, FullProjectInfo proj, String trace) {
        StringBuffer buff = new StringBuffer();
        Matcher matcher = TRACE_LINE_PATTERN.matcher(htmlEscapeStr(trace, " ", " "));

        while (matcher.find()) {
            String fqcn = matcher.group(2).replace('$','.');
            FullClassInfo clazz = (FullClassInfo)proj.findClass(fqcn);

            Logger.getInstance().debug(fqcn + " ... " + ((clazz != null) ? "found" : "CLASS NOT FOUND"));
            if (clazz != null) {
                final String pkgName = clazz.getPackage().getName();
                String lineStr = matcher.group(4);
                final String srcLineLink;
                if (LINE_NUMBER_PATTERN.matcher(lineStr).matches()) {
                    int line = Integer.parseInt(matcher.group(5));
                    srcLineLink = getSrcLineLink(false, clazz.getContainingFile(), line);
                } else {
                    StringBuffer srcLinkBuf = getSrcFileLink(false, true, clazz);
                    srcLineLink = srcLinkBuf != null ? srcLinkBuf.toString() : "";
                }
                final String escapedLine = matcher.group(0).replaceAll("\\$", "\\\\\\$");
                Logger.getInstance().debug("Linkifying: '" + escapedLine + "'");
                matcher.appendReplacement(buff, "<a href=\"" + rootRelPath + getPkgURLPath(pkgName) + srcLineLink + "\">" + escapedLine + "</a>");
            }
        }
        StringBuffer tail = new StringBuffer();
        matcher.appendTail(tail);
        Logger.getInstance().debug("Appending non-linkified tail: '" + tail + "'");
        return buff.append(tail).toString();
    }

    public String linkifyStackTrace(String rootRelPath, StackTraceInfo trace) {
        final StringBuilder buff = new StringBuilder();
        for (Object entryObj : trace.getEntries()) {
            StackTraceInfo.TraceEntry entry = (StackTraceInfo.TraceEntry) entryObj;
            buff.append("<div>");
            if (entry.isResolved()) {
                buff.append(entry.getLinePrefix());
                buff.append("<a href=\"").append(rootRelPath).
                        append(getSrcLineLink(true, entry.getContainingFile(), entry.getLineNum()));
                buff.append("\">").append(htmlEscapeStr(entry.getLinkableLineSegment(), " ", " ")).append("</a>");
            } else {
                buff.append(htmlEscapeStr(entry.getLine(), " ", " "));
            }
            buff.append("</div>");
        }
        return buff.toString();
    }


    public String getTestClassLink(boolean topLevel, ClassInfo classInfo) {
        StringBuilder outname = new StringBuilder();
        if (topLevel) {
            final String pkgURLPath = getPkgURLPath(classInfo.getPackage().getName());
            outname.append(pkgURLPath);
        }
        outname.append("test-")
                .append(classInfo.getName().replaceAll("\\W", "_"))
                .append(".html");
        return outname.toString();
    }

    /**
     * Returns a value a given percentage amount between min and max.
     * @return a value representing input along the interval min--max.
     */
    public int constrain(float input, int min, int max) {
        final int diff = max - min;
        return  min + (int)(input * diff);
    }

    public int getFontSize(StatisticsClassInfoVisitor stats, BaseClassInfo classInfo, int min, int max) {
        int result = stats.getCalculator().getScaledValue(classInfo);
        float pcResult = getFraction(result, stats.getMax());
        return constrain(pcResult, min, max);
    }

    @SuppressWarnings("unused") // cloud-body.vm, dashboard.vm
    public String getColor(StatisticsClassInfoVisitor stats, BaseClassInfo classInfo) {
        int result = stats.getCalculator().getScaledValue(classInfo);
        return ReportColors.ADG_COLORS.getStringColor(getFraction(result, stats.getMax()));
    }

    public float getFraction(float num, long dem) {
        if (dem != 0) {
            return num / dem;
        } else {
            return -1;
        }
    }

    /** Helper method used because Velocity doesn't support static method invocations */
    public Set<TestCaseInfo> tcisInHitRange(Map<TestCaseInfo, BitSet> tcisAndHits, CoverageDataRange range) {
        return CoverageData.tcisInHitRange(tcisAndHits, range);
    }

    public static String pluralize(int value, String word) {
        return Formatting.pluralizedWord(value, word);
    }

    /**
     * Replace all non-alhphanumeric characters by a blank "_".
     */
    public static String blankNonAlpha(String input) {
        return input.replaceAll("[^a-zA-Z0-9]", "_");
    }

    public static String isAre(int value) {
        return value == 1 ? "is" : "are";
    }

}
