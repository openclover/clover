package com.atlassian.clover.instr.java;

import clover.antlr.RecognitionException;
import clover.antlr.TokenStreamException;
import clover.antlr.TokenStreamRecognitionException;
import clover.com.google.common.collect.Sets;
import com.atlassian.clover.api.CloverException;
import com.atlassian.clover.api.instrumentation.ConcurrentInstrumentationException;
import com.atlassian.clover.api.instrumentation.InstrumentationSession;
import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.instr.java.module.JavaModuleInfoLexer;
import com.atlassian.clover.instr.java.module.JavaModuleInfoRecognizer;
import com.atlassian.clover.registry.entities.FullPackageInfo;
import com.atlassian.clover.util.ChecksummingReader;
import com.atlassian.clover.util.FileUtils;
import com.atlassian.clover.util.Formatting;
import com.atlassian.clover.Logger;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.MethodRegexpContext;
import com.atlassian.clover.context.NamedContext;
import com.atlassian.clover.context.StatementRegexpContext;
import com.atlassian.clover.registry.Clover2Registry;
import com.atlassian.clover.registry.entities.FullFileInfo;
import com.atlassian.clover.registry.metrics.FileMetrics;
import com.atlassian.clover.util.CloverUtils;
import com.atlassian.clover.util.IOStreamUtils;
import com.atlassian.clover.util.UnicodeDecodingReader;
import com.atlassian.clover.util.UnicodeEncodingWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Set;

public class Instrumenter {
    private final JavaInstrumentationConfig config;
    private final Logger log;

    private Clover2Registry registry;
    private InstrumentationSession session;
    private ContextTreeNode contextTreeRoot;
    private int numFiles;
    private int numClasses;
    private Set<String> packages;
    private int numMethods;
    private int numTestMethods;
    private int loc;
    private int ncloc;

    public Instrumenter(JavaInstrumentationConfig config) {
        this(Logger.getInstance(), config);
    }

    public Instrumenter(Logger log, JavaInstrumentationConfig config) {
        this.log = log;
        this.config = config;
    }

    public JavaInstrumentationConfig getConfig() {
        return config;
    }

    public void startInstrumentation() throws CloverException {
        try {
            final Clover2Registry clover2Registry = Clover2Registry.createOrLoad(config.getRegistryFile(), config.getProjectName());
            if (clover2Registry == null) {
                throw new CloverException("Unable to create or load clover registry located at: " + config.getRegistryFile());
            }
            startInstrumentation(clover2Registry);
        }
        catch (IOException e) {
            throw new CloverException(e);
        }
    }

    public void startInstrumentation(Clover2Registry reg) throws CloverException {
        resetStatistics();

        registry = reg;
        session = registry.startInstr(config.getEncoding());
        contextTreeRoot = new ContextTreeNode(reg.getContextStore().size(), new ContextSet());

        log.info("Processing files at " + config.getSourceLevel() + " source level.");
    }

    /**
     * reads the srcFile and produces an instrumented version of that file at the package path rooted at destRoot,
     * creating parent dirs as needed.
     * @param srcFile the file to instrument
     * @param destRoot the destination root dir
     * @param fileEncoding encoding of the file being instrumented, if null then a global setting from the
     *                     <code>config.getEncoding()</code> will be used
     * @return file reference to the instrumented version
     * @throws CloverException if something goes wrong
     */
    public File instrument(@NotNull final File srcFile,
                           @NotNull final File destRoot,
                           @Nullable final String fileEncoding) throws CloverException {
        if (registry == null) {
            throw new IllegalStateException("Instrumenter not initialized.");
        }

        File instrTmp = null;
        Writer out = null;
        try {
            // perform instrumentation to a tmp file
            instrTmp = File.createTempFile("clover", ".java");
            final String currentFileEncoding = fileEncoding != null ? fileEncoding : config.getEncoding();
            if (currentFileEncoding != null) {
                out = new OutputStreamWriter(new FileOutputStream(instrTmp), currentFileEncoding);
            } else {
                out = new FileWriter(instrTmp);
            }

            final InstrumentationSource fileSource = new FileInstrumentationSource(srcFile, currentFileEncoding);
            final FileStructureInfo structInfo = instrument(fileSource, out, currentFileEncoding);

            // copy file into dest
            File destDir = destRoot;
            String pkgName = structInfo.getPackageName();
            if (!FullPackageInfo.isDefaultName(pkgName)) {
                destDir = new File(destRoot, CloverUtils.packageNameToPath(pkgName, false));
            }

            if (!destDir.isDirectory() && !destDir.mkdirs()) {
                throw new CloverException("Failed to create destination path " + destDir);
            }


            String srcFileName = srcFile.getName();

            if (srcFileName.indexOf('.') > -1) {
                srcFileName =
                    srcFileName.substring(
                        0,
                        srcFile.getName().lastIndexOf('.'))
                    + '.' + config.getInstrFileExtension();
            }

            File instr = new File(destDir, srcFileName);
            FileUtils.fileCopy(instrTmp, instr);
            log.verbose("Processed '" + srcFile + "' to '" + instr + "'");
            return instr;
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
            throw new CloverException(e);
        } catch (RecognitionException e) {
            String msg = srcFile + ":" + e.getLine() + ":" +
                    e.getColumn() + ":" + e.getMessage();
            log.error(msg);
            throw new CloverException(msg, e);
        } catch (TokenStreamRecognitionException e) {
            String msg = srcFile + ":" + e.recog.getLine() + ":" +
                    e.recog.getColumn() + ":" + e.getMessage();
            log.error(msg);
            throw new CloverException(msg, e);
        } catch (TokenStreamException e) {
            log.error("Error processing " + srcFile);
            log.error(e.getMessage());
            throw new CloverException(e);
        } catch (IOException e) {
            log.error("Error processing " + srcFile);
            log.error(e.getMessage());
            throw new CloverException(e);
        } finally {
            if (instrTmp != null) {
                instrTmp.delete();
            }
            IOStreamUtils.close(out);
        }
    }

    /**
     * registers orig and produces an instrumented version of in to out
     *
     * @param in  the input file to instrument
     * @param out the destination stream
     * @param fileEncoding encoding of the file being instrumented, a <code>null</code> value means undefined
     * @return FileStructureInfo - file reference to the instrumented version
     * @throws CloverException      if something goes wrong
     * @throws TokenStreamException
     * @throws IOException
     * @throws RecognitionException
     */
    public FileStructureInfo instrument(final @NotNull InstrumentationSource in, final @NotNull Writer out,
                                        final @Nullable String fileEncoding)
            throws TokenStreamException, IOException, RecognitionException, CloverException {

        // open input stream, check if file was not instrumented already
        final BufferedReader bin = new BufferedReader(in.createReader()); // will be closed by checksummingReader.close()
        CloverTokenStreamFilter.guardAgainstDoubleInstrumentation(in.getSourceFileLocation(), bin);
        final ChecksummingReader checksummingReader = new ChecksummingReader(bin);
        final UnicodeDecodingReader unicodeReader = new UnicodeDecodingReader(checksummingReader);

        final FileStructureInfo fileStructureInfo = new FileStructureInfo(in.getSourceFileLocation());
        final CloverTokenStreamFilter filter;
        int linecount;
        int nclinecount;

        if (in.getSourceFileLocation().getName().equalsIgnoreCase("module-info.java")) {
            // parser for module-info.java files
            // create java lexer; wrap the lexer in a filter that hides whitespace
            // tokens from the parser, and collects tokens to output later
            final JavaModuleInfoLexer lexer = new JavaModuleInfoLexer(unicodeReader, config);
            filter = new CloverTokenStreamFilter(in.getSourceFileLocation().getAbsolutePath(), lexer);

            final JavaModuleInfoRecognizer parser = new JavaModuleInfoRecognizer(filter, config, fileStructureInfo, contextTreeRoot);
            parser.compilationUnit();

            // record the number of lines in this file
            linecount = lexer.getLineCount();
            nclinecount = lexer.getNCLineCount();
        } else {
            // parser for normal java files
            // create java lexer; wrap the lexer in a filter that hides whitespace
            // tokens from the parser, and collects tokens to output later
            final JavaLexer lexer = new JavaLexer(unicodeReader, config);
            filter = new CloverTokenStreamFilter(in.getSourceFileLocation().getAbsolutePath(), lexer);

            // create a parser that reads from the filtered token stream and start parsing at the compilationUnit rule
            final JavaRecognizer parser = new JavaRecognizer(filter, config, fileStructureInfo, contextTreeRoot);
            parser.compilationUnit();

            // record the number of lines in this file
            linecount = lexer.getLineCount();
            nclinecount = lexer.getNCLineCount();
        }

        // corrent EOL
        if (filter.isEOLTerminated()) {
            linecount--;
        }

        checksummingReader.close();
        unicodeReader.close();

        // every file could have it's own encoding (see IntelliJ IDEA for instance), so update current encoding for every single file
        session.setSourceEncoding(fileEncoding);
        final FullFileInfo fileInfo = (FullFileInfo) session.enterFile(
                fileStructureInfo.getPackageName(), in.getSourceFileLocation(),
                linecount, nclinecount,
                in.getSourceFileLocation().lastModified(), in.getSourceFileLocation().length(),
                checksummingReader.getChecksum());

        // actually do the instrumentation
        filter.instrument(fileStructureInfo, fileInfo, session, config);
        matchContexts(fileStructureInfo, registry.getContextStore());

        // output the instrumented file
        final Writer unicodeWriter = new UnicodeEncodingWriter(new BufferedWriter(out));
        filter.write(unicodeWriter);
        unicodeWriter.close();

        session.exitFile();

        updateStatistics(fileInfo);
        return fileStructureInfo;
    }

    /**
     * Reads the <code>charSequence</code> and returns an instrumented version of it.
     *
     * @param orig         represents a source file location associated the input <code>charSequence</code>
     * @param charSequence source text to be instrumented
     * @param fileEncoding original file encodng, a <code>null</code> value means undefined
     * @return CharSequence instrumented version of sources
     * @throws TokenStreamException
     * @throws IOException
     * @throws RecognitionException
     * @throws CloverException
     */
    public CharSequence instrument(final @NotNull File orig, final @NotNull CharSequence charSequence,
                                   final @Nullable String fileEncoding)
            throws TokenStreamException, IOException, RecognitionException, CloverException {

        final StringWriter stringWriter = new StringWriter();
        final InstrumentationSource charSequenceSource = new CharSequenceInstrumentationSource(orig, charSequence);
        // ignoring FileStructureInfo return value
        instrument(charSequenceSource, stringWriter, fileEncoding);
        return stringWriter.toString();
    }

    public Clover2Registry endInstrumentation() throws CloverException {
        return endInstrumentation(false);
    }

    public Clover2Registry endInstrumentation(boolean append) throws CloverException {
        try {
            finishAndApply(session);
            if (append) {
                registry.saveAndAppendToFile();
            } else {
                registry.saveAndOverwriteFile();
            }
            double secs = (double)(session.getEndTS() - session.getStartTs())/1000;

            int pkgs = packages.size();
            log.info("Clover all over. Instrumented "
                    + numFiles + Formatting.pluralizedWord(numFiles, " file")
                    + " ("+ pkgs + Formatting.pluralizedWord(pkgs, " package")
                    + ").");

            if (numTestMethods > 0) {
                log.info(numTestMethods + " test method" + (numTestMethods != 1 ? "s" : "") + " detected.");
            }
            log.debug("Elapsed time = " + Formatting.format3d(secs) + " secs." +
                    (secs > 0 ?  " (" + Formatting.format3d((double)numFiles / secs) + " files/sec, " +
                            Formatting.format3d((double)loc/secs)+" srclines/sec)" : ""));
            return registry;
        }
        catch (IOException e) {
            log.error("Error finalising instrumentation: ", e);
            throw new CloverException(e);
        }
    }

    protected void finishAndApply(InstrumentationSession session) throws ConcurrentInstrumentationException {
        session.close();
    }

    private void resetStatistics() {
        numFiles = 0;
        numClasses = 0;
        packages = Sets.newHashSet();
        numMethods = 0;
        numTestMethods = 0;
        loc = 0;
        ncloc = 0;
    }

    private void updateStatistics(FullFileInfo finfo) {
        numFiles++;
        FileMetrics metrics = (FileMetrics)finfo.getMetrics();
        numClasses += metrics.getNumClasses();
        packages.add(finfo.getContainingPackage().getName());
        numMethods += metrics.getNumMethods();
        numTestMethods += metrics.getNumTestMethods();
        loc += metrics.getLineCount();
        ncloc += metrics.getNcLineCount();
    }

    private void matchContexts(FileStructureInfo fileInfo, ContextStore contexts) {
        int numMarkers = fileInfo.getNumMethodMarkers();
        for (int i = 0; i < numMarkers; i++) {
            FileStructureInfo.MethodMarker marker = fileInfo.getMethodMarker(i);
            for (MethodRegexpContext ctx : contexts.getMethodContexts()) {
                if (ctx.matches(marker)) {
                    addContextToMarker(ctx, marker);
                    log.debug("Method context match, line " + marker.getStart().getLine() + ", id=" + ctx.getName());
                }
            }
        }
        numMarkers = fileInfo.getNumStatementMarkers();
        for (int i = 0; i < numMarkers; i++) {
            FileStructureInfo.Marker marker = fileInfo.getStatementMarker(i);
            for (StatementRegexpContext ctx : contexts.getStatementContexts()) {
                if (ctx.matches(marker)) {
                    addContextToMarker(ctx, marker);
                    log.debug("Statement context match, line " + marker.getStart().getLine() + ", id=" + ctx.getName());
                }
            }
        }
    }

    private void addContextToMarker(NamedContext context, FileStructureInfo.Marker marker) {
        CloverToken curr = marker.getStart();
        CloverToken end = marker.getEnd();

        while (curr != null && curr != end) {
            curr.addContext(context);
            curr = curr.getNext();
        }
        if (curr != null) {
            curr.addContext(context);
        }
    }

    public InstrumentationSession getSession() {
        return session;
    }

}
