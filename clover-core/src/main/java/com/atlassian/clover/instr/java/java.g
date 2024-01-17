header {
package com.atlassian.clover.instr.java;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.cfg.instr.java.LambdaInstrumentation;
import com.atlassian.clover.Contract;
import com.atlassian.clover.Logger;
import com.atlassian.clover.context.ContextStore;
import com.atlassian.clover.context.ContextSet;
import com.atlassian.clover.registry.*;
import com.atlassian.clover.registry.entities.*;

}

/**
 * Java 1.5/JSR14 Recognizer
 * Based on the Public Domain Java 1.3 antlr grammar provided at
 * <a href="http://www.antlr.org">www.antlr.org</a>
 */
class JavaRecognizer extends Parser;
options {
    defaultErrorHandler = false;     // Don't generate parser error handlers
    k = 2;                           // two token lookahead
    importVocab=JavaLexer;            // import JavaLexer generated from java-lexer.g
    exportVocab=Java;                // Call its vocabulary "Java"
    codeGenMakeSwitchThreshold = 100;  // Some optimizations
    codeGenBitsetTestThreshold = 100;
}

tokens {
    BLOCK; MODIFIERS; OBJBLOCK; SLIST; CTOR_DEF; METHOD_DEF; VARIABLE_DEF;
    INSTANCE_INIT; STATIC_INIT; TYPE; CLASS_DEF; INTERFACE_DEF;
    PACKAGE_DEF; ARRAY_DECLARATOR; EXTENDS_CLAUSE; IMPLEMENTS_CLAUSE;
    PARAMETERS; PARAMETER_DEF; LABELED_STAT; TYPECAST; INDEX_OP;
    POST_INC; POST_DEC; METHOD_CALL; EXPR; ARRAY_INIT;
    UNARY_MINUS; UNARY_PLUS; CASE_GROUP; ELIST; FOR_INIT; FOR_CONDITION;
    FOR_ITERATOR; EMPTY_STAT;
    SUPER_CTOR_CALL; CTOR_CALL;


}

{

    /** use to log messages **/
    private static final Logger LOG = Logger.getInstance();

    private ContextTreeNode currentContextTreeNode;
    private boolean topLevelClass = true;
    private CloverTokenStreamFilter mFilter;
    /** A stack of nested classes. We compose a full class name as a concatenation of outer classes' names. */
    private LinkedList<String> classnameList = new LinkedList<String>();
    private boolean constExpr = false;
    
    private JavaInstrumentationConfig cfg;
    private FileStructureInfo fileInfo;
    private ClassEntryNode currentTopLevelClassEntry;
    private Emitter suppressWarningsInstr = null;
    private boolean existingFallthroughSuppression = false; 

    /**
     * Counts the number of LT seen in the typeArguments production.
     * It is used in semantic predicates to ensure we have seen
     * enough closing '>' characters; which actually may have been
     * either GT, SR or BSR tokens.
     */
    private int ltCounter = 0;

    /**
     * Counts number of lambdas present in the source file in the order of their appearance. This number is later
     * used as lambda ID. Such number is more stable than using a line number, for instance.
     */
    private int lambdaCounter = 0;

    private final Deque<Deque<String>> identifiersStack = new ArrayDeque<Deque<String>>();

    public JavaRecognizer(CloverTokenStreamFilter filter, JavaInstrumentationConfig cfg, FileStructureInfo fileInfo, ContextTreeNode contextTreeRoot) {
        this(filter);
        mFilter = filter;
        this.cfg = cfg;
        this.fileInfo = fileInfo;
        this.currentContextTreeNode = contextTreeRoot;
        pushHeadIdentifierStack();
    }

    private ClassEntryNode enterClass(Modifiers mods, CloverToken tok,
            boolean aIsInterface, boolean aIsEnum, boolean isAnnotation) {

        return enterClass(null, mods, tok, aIsInterface, aIsEnum, isAnnotation, null);
    }

    private ClassEntryNode enterClass(Map<String, List<String>> tags, Modifiers mods, CloverToken tok,
            boolean aIsInterface, boolean aIsEnum, boolean isAnnotation, String superclass) {

        String classname = tok.getText();
        int startLine = tok.getLine();
        int startCol = tok.getColumn();
        classnameList.add(classname);
        // the full name of this class is the concatenation of all containing class names
        String fullName = getClassname(classnameList);
        ClassEntryNode node = new ClassEntryNode(tags, mods, fullName, fileInfo.getPackageName(), superclass,
                getCurrentContext(), startLine, startCol, topLevelClass, aIsInterface, aIsEnum, isAnnotation);
        tok.addPreEmitter(node);
        if (topLevelClass) {
            currentTopLevelClassEntry = node;
        }
        return node;
    }

     private void pushIdentifierToHeadStack(String ident) {
         headIdentifiersStack().push(ident);
     }

     private Deque<String> headIdentifiersStack() {
         return identifiersStack.getFirst();
     }

     private void popHeadIdentifierStack() {
         identifiersStack.pollFirst();
     }

     private void pushHeadIdentifierStack() {
         identifiersStack.push(new ArrayDeque<String>());
     }

    private void exitClass(CloverToken t, ClassEntryNode entry) {
        Contract.pre(classnameList.size() > 0);
        classnameList.removeLast();
        t.addPreEmitter(new ClassExitNode(entry, getClassname(classnameList), t.getLine(), t.getColumn() + t.getText().length()));
    }

    private String getClassname(List<String> classList) {
        String fullName = "";
        String sep = "";
        for (String className : classList) {
            fullName += sep + className;
            sep = ".";
        }
        return fullName;
    }

    private void enterContext(int newContext) {
        currentContextTreeNode = currentContextTreeNode.enterContext(newContext);
    }

    private ContextSet getCurrentContext() {
        return currentContextTreeNode.getContext();
    }
    
    private void exitContext() {
        currentContextTreeNode = currentContextTreeNode.exitContext();
    }

    private void instrStaticRecorderMember(boolean isEnum) {
        if (currentTopLevelClassEntry != null && currentTopLevelClassEntry.getRecorderInsertPoint() != null) {

            RecorderInstrEmitter recorderInstr = new RecorderInstrEmitter(isEnum);
            currentTopLevelClassEntry.setRecorderInstrEmitter(recorderInstr);
            if (isEnum) {
                currentTopLevelClassEntry.getRecorderInsertPoint().addPreEmitter(new SimpleEmitter(";"));
                currentTopLevelClassEntry.getRecorderInsertPoint().addPreEmitter(recorderInstr);
            } else {
                currentTopLevelClassEntry.getRecorderInsertPoint().addPostEmitter(recorderInstr);
            }
        }
    }

    private void instrSuppressWarnings(CloverToken instrPoint) {
        if (!existingFallthroughSuppression && fileInfo.isSuppressFallthroughWarnings()) {
            if (suppressWarningsInstr == null) {
                // no existing SuppressWarnings annotation on the outermost type, so add our own
                instrPoint.addPreEmitter(new SimpleEmitter("@" + cfg.getJavaLangPrefix() + "SuppressWarnings({\"fallthrough\"}) "));
            }
        }
        else if (suppressWarningsInstr != null) {
            suppressWarningsInstr.setEnabled(false);
        }
    }

    private void setRecorderMemberInsertPoint(ClassEntryNode node, CloverToken insertPoint) {
        if (topLevelClass) {
            node.setRecorderInsertPoint(insertPoint);
            topLevelClass = false;
        }
    }

    private MethodEntryInstrEmitter instrEnterMethod(MethodSignature sig, CloverToken start,
                                                     CloverToken leftCurly, CloverToken skip) {
        CloverToken instrPoint = (skip == null ? leftCurly : skip);
        MethodRegistrationNode reg = new MethodRegistrationNode(getCurrentContext(), sig, start.getLine(), start.getColumn());
        leftCurly.addPreEmitter(reg);

        MethodEntryInstrEmitter ret = new MethodEntryInstrEmitter(reg);
        instrPoint.addPostEmitter(ret);
        instrPoint.addPostEmitter(new DirectedFlushEmitter());
        return ret;
    }

    private MethodEntryInstrEmitter instrEnterMethod(MethodSignature sig, CloverToken start, CloverToken leftCurly) {
        return instrEnterMethod(sig, start, leftCurly, null);
    }

    private void instrExitMethod(MethodEntryInstrEmitter entryEmitter, CloverToken tok) {
        tok.addPreEmitter(new MethodExitInstrEmitter(entryEmitter, tok.getLine(), tok.getColumn()+tok.getText().length()));
    }

    /**
     * Instrument beginning of a lambda body in a method-like way. We shall get a code like this:
     *   before:  { }
     *   after :  { RECORDER_INSTANCE_NAME.inc(777); }
     */
    private LambdaBlockEntryEmitter instrEnterLambdaBlock(MethodSignature lambdaSignature, CloverToken leftCurly) {
        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                        || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                        || cfg.getInstrumentLambda() == LambdaInstrumentation.BLOCK) {
             LambdaBlockEntryEmitter emitter = new LambdaBlockEntryEmitter(lambdaSignature, leftCurly.getLine(),
                    leftCurly.getColumn());
             leftCurly.addPostEmitter(emitter);
             return emitter;
        }
        return null;
    }

    /**
     * Instrument ending of a lambda body. It does not do anything currently (we might add coverage flushing in future)
     */
    private void instrExitLambdaBlock(LambdaBlockEntryEmitter entryEmitter, CloverToken rightCurly) {
        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                    || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                    || cfg.getInstrumentLambda() == LambdaInstrumentation.BLOCK) {
            rightCurly.addPreEmitter(new LambdaBlockExitEmitter(entryEmitter, rightCurly.getLine(),
                    rightCurly.getColumn() + rightCurly.getText().length()));
        }
    }

    /**
     * Instrument beginning of a lambda expression. We shall get a code like this:
     *   before:   [() -> 1 + 2]
     *   after :   [RECORDER_INSTANCE_NAME.lambdaInc(777, () -> 1 + 2]
     * <p/>
     * or in case when class cast is also present:
     *   before:   [(Integer)() -> 1 + 2]
     *   after :   [(Integer)RECORDER_INSTANCE_NAME.lambdaInc(777, (Integer)() -> 1 + 2]
     * <p/>
     * Tokens:
     * <pre>
     *   (Integer)(x, y) -> x + y
     *            ^ lambdaStart
     *                      ^ bodyStart
     *   ^ classCastStart
     *           ^ classCastEnd
     * </pre>
     */
    private LambdaExpressionEntryEmitter instrEnterLambdaExpression(MethodSignature lambdaSignature,
            CloverToken lambdaStart, CloverToken bodyStart,
            CloverToken classCastStart, CloverToken classCastEnd) {
        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                     || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                     || cfg.getInstrumentLambda() == LambdaInstrumentation.EXPRESSION) {
            String classCast = "";
            if (classCastStart != null && classCastEnd != null && classCastStart != classCastEnd) {
                classCast = TokenListUtil.getNormalisedSequence(classCastStart, classCastEnd);
            }
            LambdaExpressionEntryEmitter emitter = new LambdaExpressionEntryEmitter(lambdaSignature, classCast,
                    lambdaStart.getLine(), lambdaStart.getColumn(), bodyStart.getLine(), bodyStart.getColumn());
            lambdaStart.addPreEmitter(emitter);
            return emitter;
        }
        return null;
    }

     /**
      * Instrument ending of a lambda expression. We shall get a code like this:
      *   before:   [() -> 1 + 2]
      *   after :   [() -> 1 + 2)]
      */
    private void instrExitLambdaExpression(LambdaExpressionEntryEmitter entryEmitter, CloverToken tok) {
         if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                         || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                         || cfg.getInstrumentLambda() == LambdaInstrumentation.EXPRESSION) {
             tok.addPostEmitter(new LambdaExpressionExitEmitter(entryEmitter, tok.getLine(), tok.getColumn()+tok.getText().length()));
         }
    }

    /**
     * Instrument beginning of a lambda method reference. We shall get a code like this:
     *   before:   [Math::sum]
     *   after :   [RECORDER_INSTANCE_NAME.lambdaInc(777, Math::abs]
     * <p/>
     * or in case when class cast is also present:
     *   before:   [(Function<Integer,Integer>)Math::abs]
     *   after :   [(Function<Integer,Integer>)RECORDER_INSTANCE_NAME.lambdaInc(777, (Function<Integer,Integer>)Math::abs]
     * <p/>
     * Tokens:
     * <pre>
     *   (Function<Integer,Integer>)Math::abs
     *                              ^ methodReferenceStart
     *
     *   ^ classCastStart
     *                             ^ classCastEnd
     * </pre>
     */
    private LambdaExpressionEntryEmitter instrEnterLambdaMethodReference(MethodSignature lambdaSignature,
            CloverToken methodReferenceStart, CloverToken classCastStart, CloverToken classCastEnd) {
        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL) {
            String classCast = "";
            if (classCastStart != null && classCastEnd != null && classCastStart != classCastEnd) {
                classCast = TokenListUtil.getNormalisedSequence(classCastStart, classCastEnd);
            }
            LambdaExpressionEntryEmitter emitter = new LambdaExpressionEntryEmitter(lambdaSignature, classCast,
                    methodReferenceStart.getLine(), methodReferenceStart.getColumn(),
                    methodReferenceStart.getLine(), methodReferenceStart.getColumn());
            methodReferenceStart.addPreEmitter(emitter);
            return emitter;
        }
        return null;
    }

     /**
      * Instrument ending of a lambda expression. We shall get a code like this:
      *   before:   [Math::abs]
      *   after :   [Math::abs)]
      */
    private void instrExitMethodReference(LambdaExpressionEntryEmitter entryEmitter, CloverToken tok) {
         if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL) {
             tok.addPostEmitter(new LambdaExpressionExitEmitter(entryEmitter, tok.getLine(), tok.getColumn()+tok.getText().length()));
         }
    }

    /**
     * Instrument beginning of a lambda expression, which shall be transformed into lambda block. We shall get a code like this:
     *   before:   [() -> 1 + 2]
     *   after :   [() -> {RECORDER_INSTANCE_NAME.inc(777); return 1 + 2]
     * <p/>
     * or in case when class cast is also present:
     *   before:   [(Integer)() -> 1 + 2]
     *   after :   [(Integer)() -> {RECORDER_INSTANCE_NAME.inc(777); return 1 + 2]
     * <p/>
     *  Return key word will be skipped if lambda expression is noted as void return type by CLOVER:VOID directive, like this:
     *
     *  [() -> /*CLOVER:VOID"&#42;&#47;" System.out.println("Hello World!")]
     *
     * Tokens:
     * <pre>
     *   (x, y) -> x + y
     *   ^ lambdaStart
     *             ^ bodyStart
     * </pre>
     */
     private LambdaExprToBlockStartEntryEmitter instrEnterLambdaExprToBlockExpression(MethodSignature lambdaSignature,
            CloverToken lambdaStart, CloverToken bodyStart) {

        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                || cfg.getInstrumentLambda() == LambdaInstrumentation.EXPRESSION) {
             LambdaExprToBlockStartEntryEmitter startEmitter = new LambdaExprToBlockStartEntryEmitter(
                    lambdaSignature, lambdaStart.getLine(), lambdaStart.getColumn());
             LambdaExprToBlockBodyEntryEmitter bodyEmitter = new LambdaExprToBlockBodyEntryEmitter(
                    startEmitter, bodyStart.getLine(), bodyStart.getColumn());
             lambdaStart.addPreEmitter(startEmitter);
             bodyStart.addPreEmitter(bodyEmitter);
             return startEmitter;
         }
         return null;
     }


    /**
     * Instrument ending of a lambda expression, which shall be transformed into lambda block. We shall get a code like this:
     *   before:   [() -> 1 + 2]
     *   after :   [() -> 1 + 2;}]
     */
    private void instrExitLambdaExprToBlockExpression(LambdaExprToBlockStartEntryEmitter entryEmitter, CloverToken tok) {
        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                     || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                     || cfg.getInstrumentLambda() == LambdaInstrumentation.EXPRESSION) {
             tok.addPostEmitter(
                    new LambdaExprToBlockExitEmitter(entryEmitter, tok.getLine(), tok.getColumn()+tok.getText().length()));
         }
    }

    private CaseExpressionEntryEmitter instrEnterCaseExpression(CloverToken insertionPoint, CloverToken endToken, ContextSet context, int complexity) {
        // we add "caseInc(123,()->" AFTER the "->"
        final CaseExpressionEntryEmitter entryEmitter = new CaseExpressionEntryEmitter(
                context,
                insertionPoint.getLine(),
                insertionPoint.getColumn(),
                endToken.getLine(),
                endToken.getColumn(),
                complexity);
        insertionPoint.addPostEmitter(entryEmitter);
        return entryEmitter;
    }

    private void instrExitCaseExpression(CaseExpressionEntryEmitter entryEmitter, CloverToken insertionPoint) {
        // we add closing ")" BEFORE the ";"
        insertionPoint.addPreEmitter(
                new CaseExpressionExitEmitter(entryEmitter));
    }

    private CaseThrowExpressionEntryEmitter instrEnterCaseThrowExpression(CloverToken insertionPoint, CloverToken endToken, ContextSet context, int complexity) {
        // we add "{ R.inc();" AFTER the "->"
        final CaseThrowExpressionEntryEmitter entryEmitter = new CaseThrowExpressionEntryEmitter(
                context,
                insertionPoint.getLine(),
                insertionPoint.getColumn(),
                endToken.getLine(),
                endToken.getColumn(),
                complexity);
        insertionPoint.addPostEmitter(entryEmitter);
        return entryEmitter;
    }

    private void instrExitCaseThrowExpression(CaseThrowExpressionEntryEmitter entryEmitter, CloverToken insertionPoint) {
        // we add closing "}" AFTER the ";"
        insertionPoint.addPostEmitter(
                new CaseThrowExpressionExitEmitter(entryEmitter));
    }

    private CloverToken maybeAddFlushInstr(CloverToken last) {
        last.addPostEmitter(new DirectedFlushEmitter());
        return last;
    }

    private FlagDeclEmitter declareFlagBefore(CloverToken tok) {
        FlagDeclEmitter flag = new FlagDeclEmitter();
        tok.addPreEmitter(flag);
        return flag;
    }

    private CloverToken instrInlineAfter(CloverToken instr, CloverToken start, CloverToken end) {
        if (cfg.isStatementInstrEnabled()) {
            instr.addPostEmitter(
                    new StatementInstrEmitter(
                            getCurrentContext(), start.getLine(), start.getColumn(), end.getLine(),
                            end.getColumn() + end.getText().length()));
            instr.addPostEmitter(new DirectedFlushEmitter());
            fileInfo.addStatementMarker(start, end);
        }
        return instr;
    }

    // same as above, but protected by a flag check
    private CloverToken instrInlineAfter(CloverToken tok, CloverToken start, CloverToken end, FlagDeclEmitter flag) {
        if (cfg.isStatementInstrEnabled()) {
            tok.addPostEmitter(
                    new FlaggedInstrEmitter(
                        flag,
                        new StatementInstrEmitter(
                                getCurrentContext(), start.getLine(), start.getColumn(),
                                end.getLine(), end.getColumn() + end.getText().length())));
            fileInfo.addStatementMarker(start, end);
        }
        return tok;
    }

    private CloverToken instrInlineBefore(CloverToken start, CloverToken end, ContextSet context, int complexity) {
        if (cfg.isStatementInstrEnabled()) {
            start.addPreEmitter(
                    new StatementInstrEmitter(
                                context, start.getLine(), start.getColumn(), end.getLine(),
                                end.getColumn() + end.getText().length(), complexity));
        }
        return start;
    }

    private void insertAutoCloseableClassDecl(CloverToken tryBlock) {
        if (cfg.isStatementInstrEnabled()) {
            tryBlock.addPreEmitter(
                new AutoCloseableEmitter());
        }
    }

    private void instrArmDecl(CloverToken start, CloverToken end, ContextSet context) {
        if (cfg.isStatementInstrEnabled()) {
          start.addPreEmitter(
            new ArmInstrEmitter(
              context, start.getLine(), start.getColumn(), end.getLine(),
              end.getColumn() + end.getText().length(), 1 /*TODO*/));
        }
    }

    private void addCloseBraceAfter(CloverToken tok) {
        // inserts a top-level token into the token stream, rather than using instrumentation
        CloverToken closeBrace = new CloverToken(JavaTokenTypes.RCURLY, "}");
        closeBrace.setLine(tok.getLine());
        closeBrace.setColumn(tok.getColumn());
        closeBrace.setPrev(tok);
        closeBrace.setNext(tok.getNext());
        tok.setNext(closeBrace);
    }

    private void addOpenBraceAfter(CloverToken tok) {
        // inserts a top-level token into the token stream, rather than using instrumentation
        CloverToken openBrace = new CloverToken(JavaTokenTypes.LCURLY, "{");
        openBrace.setLine(tok.getLine());
        openBrace.setColumn(tok.getColumn());
        openBrace.setPrev(tok);
        openBrace.setNext(tok.getNext());
        tok.setNext(openBrace);
    }

    private CloverToken instrBoolExpr(CloverToken begin, CloverToken end) {
        if (cfg.isStatementInstrEnabled()) {
          // if there is an assignment in this boolean expr, then
          // instrumentation may cause a compilation failure if the variable
          // being assigned to is not initialised prior to this expression.
          ExpressionInfo expr = ExpressionInfo.fromTokens(begin, end);
          if (!expr.isConstant()) {
              begin.addPreEmitter(new StartBoolInstrEmitter(expr));
              end.addPreEmitter(
                    new EndBoolInstrEmitter(getCurrentContext(), begin.getLine(), begin.getColumn() - 1,
                            end.getLine(), end.getColumn() + end.getText().length(), expr));
          }
        }
        return end;
    }

    private boolean maybeEnterDeprecated(CloverToken startOfBlock) {
        Map<String, List<String>> tags  = TokenListUtil.getJDocTagsAndValuesOnBlock(startOfBlock);
        boolean deprecated = tags.containsKey("deprecated");
        if (deprecated) {
            enterContext(ContextStore.CONTEXT_DEPRECATED);
        }
        return deprecated;
    }

    private boolean maybeEnterDeprecated(Map tags, Modifiers mods) {
        boolean deprecated = tags.containsKey("deprecated") || mods.containsAnnotation("Deprecated");
        if (deprecated) {
            enterContext(ContextStore.CONTEXT_DEPRECATED);
        }
        return deprecated;
    }

    private void maybeExitDeprecated(boolean dep) {
       if (dep) {
            exitContext();
       }
    }

    /**
     * A helper method to check a currently matched token.
     * Usage: <pre>{isCurrentKeyword("abc")}?</pre>
     */
    private boolean isCurrentKeyword(String keyword) throws TokenStreamException {
        return LT(0).getText().equals(keyword);
    }

    /**
     * A helper method to lookup a next token, this way avoiding writing a string literal in a grammar rule,
     * which otherwise would have been interpreted by ANTLR as a token (a reserved keyword). Use it when a
     * "pseudo-keyword" (like "record", "sealed", "module") have to be checked in a given context.
     * Usage: <pre>{isNextKeyword("abc")}? IDENT</pre>
     */
    private boolean isNextKeyword(String keyword) throws TokenStreamException {
        return LT(1).getText().equals(keyword);
    }

    /**
     * Like above, but looks up two tokens.
     * Sample usage: <pre>{isNextKeyword("abc", "def")}? IDENT IDENT</pre>
     */
    private boolean isNextKeyword(String firstKeyword, String secondKeyword) throws TokenStreamException {
        return LT(1).getText().equals(firstKeyword) && LT(2).getText().equals(secondKeyword);
    }

    /**
     * Returns n-th token from LT(n) casted to CloverToken
     */
    private CloverToken lt(int i) throws TokenStreamException {
        return (CloverToken) LT(i);
    }

    /**
     * Casts Token into CloverToken
     */
    private CloverToken ct(Token token) {
        return (CloverToken) token;
    }
}

//
// module-info.java grammar
//
// Note: none of the keywords used in module-info (module, exports, requires etc) is not a "real keyword".
// You can still use them as identifiers. Therefore, we cannot use string literals explicitly, e.g.
//     "xyz" identifier SEMI
// because string literal will become a token and "steal" this keyword from IDENT, failing on parsing regular code.
// Instead of this we use a trick to to sneak peek the next token and treat it as a simple IDENT
//      { isNextKeyword("xyz") }? IDENT identifier SEMI
//

//
// "(annotations) (open) module <identifier> {"
//
moduleDeclarationPredicate
{
    AnnotationImpl an;
    String moduleName;
}
    :
        ( an=annotation )*
        (
            { isNextKeyword("open", "module") }? IDENT IDENT
        |
            { isNextKeyword("module") }? IDENT
        )
        moduleName=identifier
        LCURLY
    ;

moduleDeclaration
{
    AnnotationImpl an;
    String moduleName;
}
    :
        ( an=annotation )*
        (
            { isNextKeyword("open") }? IDENT
        )?
        { isNextKeyword("module") }? IDENT
        moduleName=identifier
        LCURLY
        ( moduleDirective )*
        RCURLY
    ;

moduleDirective
    :
        (
            (requiresDirective)=>
            requiresDirective
        |
            (exportsDirective)=>
            exportsDirective
        |
            (opensDirective)=>
            opensDirective
        |
            (usesDirective)=>
            usesDirective
        |
            (providesDirective)=>
            providesDirective
        )
    ;

//
// "requires (transitive|static) <identifier>;"
//
requiresDirective
{
    String requiredModule;
}
    :
        { isNextKeyword("requires") }? IDENT
        (
            { isNextKeyword("transitive") }? IDENT
        |
            STATIC   // static is java keyword so we don't use a trick like above
        )?
        requiredModule=identifier
        SEMI!
    ;

//
// "exports <identifier> (to <identifier>, <identifier>);"
//
exportsDirective
{
    String exportedPackage;
    String moduleName;
}
    :
        { isNextKeyword("exports") }? IDENT
        exportedPackage=identifier
        (
            { isNextKeyword("to") }? IDENT
            moduleName=identifier
            (
                COMMA!
                moduleName=identifier
            )*
        )?
        SEMI!
    ;

//
// "opens <identifier> (to <identifier>, <identifier>);"
//
opensDirective
{
    String openedPackage;
    String moduleName;
}
    :
        { isNextKeyword("opens") }? IDENT
        openedPackage=identifier
        (
            { isNextKeyword("to") }? IDENT
            moduleName=identifier
            (
                COMMA!
                moduleName=identifier
            )*
        )?
        SEMI!
    ;

//
// "uses <identifier>;"
//
usesDirective
{
    String serviceName;
}
    :
        { isNextKeyword("uses") }? IDENT
        serviceName=identifier
        SEMI!
    ;

//
// "provides <identifier> with <identifier>, <identifier>;"
//
providesDirective
{
    String serviceName;
    String withType;
}
    :
        { isNextKeyword("provides") }? IDENT
        serviceName=identifier
        { isNextKeyword("with") }? IDENT
        withType=identifier
        (
            COMMA!
            withType=identifier
        )*
        SEMI!
    ;


// Compilation Unit: In Java, this is a single file.  This is the start
//   rule for this parser
compilationUnit
    :
        // A compilation unit starts with an optional package definition
        (   (packageDefinition)=> packageDefinition
            // need above syntactic predicate to dis-amb the 'annotation' leading both
            // packageDefinition and typeDefinition
        |
            /* nothing */
        )

        // Next we have a series of zero or more import statements
        ( importDefinition )*

        // JLS specifies two kinds of compilation unit: ordinary and modular, but we can keep it simple and just
        // have module declaration as an alternative to declarations of types
        (
            (moduleDeclarationPredicate) =>
            moduleDeclaration
        |
            // Wrapping things up with any number of class or interface
            //    definitions
            ( typeDefinition[false]
                {
                    topLevelClass=true;
                    existingFallthroughSuppression = false;
                }
            )*
        )

        EOF!
    ;


// Package statement: "package" followed by an identifier.
packageDefinition
    options {defaultErrorHandler = false;}
{
    String pn = "";
    AnnotationImpl ann = null;
}
    :
        (ann=annotation)*
        PACKAGE pn=identifier SEMI! {fileInfo.setPackageName(pn);}
    ;


// Import statement: import followed by a package or class name
//    or a "static" method import
importDefinition
    options {defaultErrorHandler = false;}
    :
        IMPORT (STATIC)? identifierStar SEMI!
    ;

// A type definition in a file is either a class or interface definition.
typeDefinition[boolean nested]
    options {defaultErrorHandler = false;}
{
    CloverToken first = null;
    Modifiers mods = null;
}
    :
        {
            pushHeadIdentifierStack(); // for the case when identifiers are recognized outside expression context
                                       // (i.e. annotations)
                                       // we don't want to fail on empty stack
            first = lt(1);
        }

        // disambiguation to recognize a difference between e.g. top-level record (IDENT) and a variable (IDENT)
        ( classOrInterfaceModifiers[false] ( CLASS | INTERFACE | AT INTERFACE | ENUM | { isCurrentKeyword("record") }? ) ) =>
        mods=classOrInterfaceModifiers[!nested]!
        typeDefinition2[mods, first, nested]
    |
        SEMI!
        {
            popHeadIdentifierStack();
        }
    ;

typeDefinition2[Modifiers mods, CloverToken first, boolean nested]
{
    boolean isEnum = false;
    String name = null;
}
    :
        (
            name=classDefinition[mods]
        |
            ( { isCurrentKeyword("record") }? ) => name=recordDefinition[mods]
        |
            name=interfaceDefinition[mods]
        |
            name=enumDefinition[mods] {isEnum=true;}
        |
            name=annotationTypeDeclaration[mods]
        )
        {
            if (!nested) {
                instrStaticRecorderMember(isEnum);
                instrSuppressWarnings(first);
            }
        }

    ;

/**
 * A declaration is the creation of a reference or primitive-type variable
 * Create a separate Type/Var tree for each var in the var list.
 */
declaration!
{
    Modifiers mods = null;
    String type = null;
}
    :
        mods=fieldModifiers[false] type=typeSpec variableDefinitions
    ;

// A type specification is a type name with possible brackets afterwards
//   (which would make it an array type).
typeSpec returns [String spec]
{
    AnnotationImpl ann = null;
}
    :
        ( options { greedy=true; }: ann=annotation )*
        (
            spec = classTypeSpec
        |
            spec = builtInTypeSpec
        )
    ;

arraySpecOpt returns [String brackets]
{
    brackets = "";
    AnnotationImpl ann = null;
}
    :
        (
            options{ greedy=true; }: // match as many as possible
            (ann=annotation)*
            LBRACK RBRACK
            {
                brackets += "[]";
            }
        )*
    ;

// A class type specification is a class type with either:
// - possible brackets afterwards
//   (which would make it an array type).
// - generic type arguments after
classTypeSpec returns [String typeSpec]
{
    String arrayOpt = "";
}
    :
        typeSpec = classOrInterfaceType
        arrayOpt = arraySpecOpt
        {
            typeSpec += arrayOpt;
        }
    ;

classOrInterfaceType returns [String type]
{
    CloverToken first = null;
    CloverToken last = null;
    type = null;
    AnnotationImpl ann = null;
}
    :
        {
            first = lt(1);
        }
        (ann=annotation)*
        IDENT
        (typeArguments)?
        (
            options{greedy=true;}: // match as many as possible
            DOT
            IDENT (typeArguments)?
        )*
        {
            last = lt(0);
            type = TokenListUtil.getNormalisedSequence(first, last);
        }
    ;

typeArguments
{
    int currentLtLevel = 0;
}
    :
        {
            currentLtLevel = ltCounter;
        }
        LT
        {
            ltCounter++;
        }
        (
            options{ generateAmbigWarnings=false; }:

            singleTypeArgument
            (
                options{ greedy=true; }: // match as many as possible
                COMMA singleTypeArgument
            )*
        )?

        (
            // turn warning off since Antlr generates the right code,
            // plus we have our semantic predicate below
            options{ generateAmbigWarnings=false; }:
            typeArgumentsEnd
        )?

        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?
    ;

singleTypeArgument
{
    String type = null;
    AnnotationImpl ann = null;
}
    :
        (
            options { greedy=true; }:
            ann=annotation
        )*
        (
            type=classTypeSpec
        |
            type=builtInTypeSpec
        |
            QUESTION
        )

        (
            // I'm pretty sure Antlr generates the right thing here:
            options{ generateAmbigWarnings=false; }:
            (
                EXTENDS
            |
                SUPER
            )
            (
                options { greedy=true; }:
                ann=annotation
            )*
            (
                type=classTypeSpec
            |
                type=builtInTypeSpec
            |
                QUESTION
            )
        )?
    ;

// this gobbles up *some* amount of '>' characters, and counts how many
// it gobbled.
protected typeArgumentsEnd
    :
        GT { ltCounter-=1; }
    |
        SR { ltCounter-=2; }
    |
        BSR { ltCounter-=3; }
    ;

// A builtin type specification is a builtin type with possible brackets
// afterwards (which would make it an array type).
builtInTypeSpec returns [String spec]
{
    String arrayOpt = "";
}
    :
        spec = builtInType
        arrayOpt = arraySpecOpt
        {
            spec += arrayOpt;
        }
    ;

// A type name. which is either a (possibly qualified and parameterized)
// class name or a primitive (builtin) type
type
{
    String spec = null;
    AnnotationImpl ann = null;
}
    :
    (
        options { greedy=true; }:
        ann=annotation
    )*
    (
        spec=classOrInterfaceType
    |
        spec=builtInType
    )
    ;

// The primitive types.
builtInType returns [String type]
{
    type = "";
}
    :
    (
        VOID
    |
        BOOLEAN
    |
        BYTE
    |
        CHAR
    |
        SHORT
    |
        INT
    |
        FLOAT
    |
        LONG
    |
        DOUBLE
    )
    {
        type = LT(0).getText();
    }
    ;

// A (possibly-qualified) java identifier.  We start with the first IDENT
//   and expand its name by adding dots and following IDENTS
// returns the identifier as a string (with all the dots)
identifier returns [String str]
{
    StringBuffer buf = new StringBuffer();
    str = null;
}
    :
        i1:IDENT  { buf.append(i1.getText()); }
        (
            DOT      { buf.append('.'); }
            i2:IDENT { buf.append(i2.getText()); }
        )*

        {
            str = buf.toString();
        }
    ;

identifierStar
    :
        IDENT
        (
            options { greedy=true; }:
            DOT IDENT
        )*
        (
            DOT STAR
        )?
    ;

/**
 * A list of zero or more modifiers (including annotations) in the class, enum or interface signature.
 */
classOrInterfaceModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    long m = 0;
    AnnotationImpl ann = null;
}
    :
        (
            options{ generateAmbigWarnings=false; }:
            m=classOrInterfaceModifier { mods.setMask(mods.getMask() | m); }
        |
            ann=annotation2[instrSuppressWarnings] { mods.addAnnotation(ann); }
        )*
    ;

/**
 * Keywords allowed for a class or interface modifier.
 */
classOrInterfaceModifier returns [long m]
{
    m = 0;
}
    :
        PRIVATE       { m=java.lang.reflect.Modifier.PRIVATE; }
    |
        PROTECTED     { m=java.lang.reflect.Modifier.PROTECTED; }
    |
        PUBLIC        { m=java.lang.reflect.Modifier.PUBLIC; }
    |
        ABSTRACT       { m=java.lang.reflect.Modifier.ABSTRACT; }
    |
        // for classes only
        FINAL         { m=java.lang.reflect.Modifier.FINAL; }
    |
        STATIC        { m=java.lang.reflect.Modifier.STATIC; }
    |
        STRICTFP      { m=java.lang.reflect.Modifier.STRICT; }
    |
        // non-sealed is treated like a keyword!
        // a workaround, because otherwise it would see "non-sealed" as IDENT-MINUS-IDENT
        NON_SEALED
        {
            m = com.atlassian.clover.registry.entities.ModifierExt.NON_SEALED;
        }
    |
        // sealed is treated like an identifier
        { isNextKeyword("sealed") }? IDENT
        {
            m = com.atlassian.clover.registry.entities.ModifierExt.SEALED;
        }
    |
        // although "record" it's not a class or interface modifier, but marks the record class (so it's
        // more like the "class" keyword, because of the fact that we treat it as IDENT,
        // we must process it in this rule, together with "sealed" to distinguish them in the token stream
        { isNextKeyword("record") }? IDENT
        {
            m = com.atlassian.clover.registry.entities.ModifierExt.RECORD;
        }
    ;

/**
 * A list of zero or more modifiers in the field signature, including annotations.
 */
fieldModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    long m = 0;
    AnnotationImpl ann = null;
}
    :
        (
            options{ generateAmbigWarnings=false; }:
            m=fieldModifier { mods.setMask(mods.getMask() | m); }
        |
            ann=annotation2[instrSuppressWarnings] { mods.addAnnotation(ann); }
        )*
    ;

/**
 * Keywords allowed for a field modifier
 */
fieldModifier returns [int m]
{
    m = 0;
}
    :
        PRIVATE       { m=java.lang.reflect.Modifier.PRIVATE; }
    |
        PROTECTED     { m=java.lang.reflect.Modifier.PROTECTED; }
    |
        PUBLIC        { m=java.lang.reflect.Modifier.PUBLIC; }
    |
        FINAL         { m=java.lang.reflect.Modifier.FINAL; }
    |
        STATIC        { m=java.lang.reflect.Modifier.STATIC; }
    |
        TRANSIENT     { m=java.lang.reflect.Modifier.TRANSIENT; }
    |
        VOLATILE      { m=java.lang.reflect.Modifier.VOLATILE; }
    ;

methodSignature [Map tags, CloverToken first, boolean isPredicate] returns [MethodSignatureExt signatureExt]
{
    Modifiers mods = null;
    boolean deprecated = false;
    String typeParam = null;
    String returnType = "";
    Parameter [] parameters = null;
    String brackets = "";
    String [] throwsTypes = null;
    signatureExt = MethodSignatureExt.of(null, null, null);
}
    :
        mods=methodModifiers[false]

        { if (!isPredicate) { deprecated = maybeEnterDeprecated(tags, mods); } }

        (typeParam=typeParameters)?

        // return type of the method
        returnType=typeSpec

        // the name of the method
        methodName:IDENT

        // parse the formal parameter declarations.
        LPAREN! parameters=parameterDeclarationList RPAREN!

        brackets = declaratorBrackets

        // get the list of exceptions that this method is
        // declared to throw
        (throwsTypes=throwsClause)?
        {
            CloverToken endSig = lt(0);
            MethodSignature signature = new MethodSignature(first, ct(methodName), endSig, tags, mods,
                    methodName.getText(), typeParam, returnType + brackets, parameters, throwsTypes);
            signatureExt = MethodSignatureExt.of(signature, endSig, deprecated);
        }
    ;

// This is the signature of a constructor.  It includes the name and parameters for the constructor.
// This also watches for a list of exception classes in a "throws" clause.
standardConstructorSignature [Map tags, CloverToken first, boolean isPredicate] returns [MethodSignatureExt signatureExt]
{
    Modifiers mods = null;
    boolean deprecated = false;
    String typeParam = null;
    Parameter [] params = null;
    String [] throwsTypes = null;
    signatureExt = MethodSignatureExt.of(null, null, null);
}
    :
        mods=constructorModifiers[false]

        { if (!isPredicate) { deprecated = maybeEnterDeprecated(tags, mods); } }

        (typeParam=typeParameters)?

        // name of the constructor
        constructorName:IDENT

        // parse the formal parameter declarations.
        LPAREN! params=parameterDeclarationList RPAREN!

        // get the list of exceptions that this method is declared to throw
        (throwsTypes=throwsClause)?
        {
            CloverToken endSig = lt(0);
            MethodSignature signature = new MethodSignature(first, ct(constructorName), endSig, tags, mods,
                    constructorName.getText(), null, null, params, throwsTypes);
            signatureExt = MethodSignatureExt.of(signature, endSig, deprecated);
        }
    ;

// This is the signature of compact canonical constructor which is allowed in records.
compactConstructorSignature [Map tags, CloverToken first, boolean isPredicate] returns [MethodSignatureExt signatureExt]
{
    Modifiers mods = null;
    boolean deprecated = false;
    String typeParam = null;
    signatureExt = MethodSignatureExt.of(null, null, null);
}
    :
        mods=constructorModifiers[false]

        { if (!isPredicate) { deprecated = maybeEnterDeprecated(tags, mods); } }

        (typeParam=typeParameters)?

        // just name of the constructor
        constructorName:IDENT
        {
            CloverToken endSig = lt(0);
            MethodSignature signature = new MethodSignature(first, ct(constructorName), endSig, tags, mods,
                    constructorName.getText(), null, null, null, null);
            signatureExt = MethodSignatureExt.of(signature, endSig, deprecated);
        }
    ;

/**
 * A list of zero or more modifiers in the constructor signature, including annotations.
 */
constructorModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    long m = 0;
    AnnotationImpl ann = null;
}
    :
        (
            options{ generateAmbigWarnings=false; }:
            m=constructorModifier { mods.setMask(mods.getMask() | m); }
        |
            ann=annotation2[instrSuppressWarnings] { mods.addAnnotation(ann); }
        )*
    ;

/**
 * Keywords allowed for a constructor modifier
 */
constructorModifier returns [int m]
{
    m = 0;
}
    :
        PRIVATE       { m=java.lang.reflect.Modifier.PRIVATE; }
    |
        PROTECTED     { m=java.lang.reflect.Modifier.PROTECTED; }
    |
        PUBLIC        { m=java.lang.reflect.Modifier.PUBLIC; }
    ;


/**
 * A list of zero or more modifiers in the method signature, including annotations.
 */
methodModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    long m = 0;
    AnnotationImpl ann = null;
}
    :
        (
            options{ generateAmbigWarnings=false; }:
            m=methodModifier { mods.setMask(mods.getMask() | m); }
        |
            ann=annotation2[instrSuppressWarnings] { mods.addAnnotation(ann); }
        )*
    ;

/**
 * Keywords allowed for a method modifier
 */
methodModifier returns [long m]
{
    m = 0;
}
    :
        PRIVATE       { m=java.lang.reflect.Modifier.PRIVATE; }
    |
        PROTECTED     { m=java.lang.reflect.Modifier.PROTECTED; }
    |
        PUBLIC        { m=java.lang.reflect.Modifier.PUBLIC; }
    |
        ABSTRACT      { m=java.lang.reflect.Modifier.ABSTRACT; }
    |
        FINAL         { m=java.lang.reflect.Modifier.FINAL; }
    |
        NATIVE        { m=java.lang.reflect.Modifier.NATIVE; }
    |
        STATIC        { m=java.lang.reflect.Modifier.STATIC; }
    |
        STRICTFP      { m=java.lang.reflect.Modifier.STRICT; }
    |
        SYNCHRONIZED  { m=java.lang.reflect.Modifier.SYNCHRONIZED; }
    |
        // not a true modifier, used only to mark virtual extension method in an interface, we keep information about it
        DEFAULT       { m=com.atlassian.clover.registry.entities.ModifierExt.DEFAULT; }
    ;

// Definition of a Java class
classDefinition! [Modifiers mods] returns [String classname]
{
	CloverToken first = lt(0);
	Map<String, List<String>> tags = null;
    boolean deprecated = false;
    CloverToken endOfBlock = null;
    String superclass = null;
    ClassEntryNode classEntry = null;
    classname = null;
    String typeParam = null;
}
    :
        CLASS
        {
            tags = TokenListUtil.getJDocTagsAndValuesOnBlock(first);
            deprecated = maybeEnterDeprecated(first);
        }
        id:IDENT

        // it _might_ have type parameters
        (typeParam=typeParameters)?

        // it _might_ have a superclass...
        superclass = superClassClause
        // it might implement some interfaces...
        implementsClause

        // if a class is sealed then it must have the permits clause
        permitsClause[mods]

        {
            classEntry = enterClass(tags, mods, ct(id), false, false, false, superclass);
        }

        // now parse the body of the class
        endOfBlock = classBlock[classEntry]
        {
            exitClass(endOfBlock, classEntry); maybeExitDeprecated(deprecated);
            classname = id.getText();
        }
    ;

// Definition of a record
recordDefinition! [Modifiers mods] returns [String recordName]
{
    CloverToken first = lt(0);
    Map<String, List<String>> tags = null;
    boolean deprecated = false;
    CloverToken endOfBlock = null;
    String superclass = null;
    ClassEntryNode classEntry = null;
    recordName = null;
    String typeParam = null;
    Parameter [] parameters = null;
}
    :
        // the "record" IDENT has been already matched in the classOrInterfaceModifier rule
        { (mods.getMask() & com.atlassian.clover.registry.entities.ModifierExt.RECORD) != 0 }?
        {
            tags = TokenListUtil.getJDocTagsAndValuesOnBlock(first);
            deprecated = maybeEnterDeprecated(first);
        }
        id:IDENT
        LPAREN! parameters=parameterDeclarationList RPAREN!

        // it _might_ have a superclass...
        superclass = superClassClause

        // it might implement some interfaces...
        implementsClause
        {
            classEntry = enterClass(tags, mods, ct(id), false, false, false, superclass);
        }
        // now parse the body of the class
        endOfBlock = classBlock[classEntry]
        {
            exitClass(endOfBlock, classEntry); maybeExitDeprecated(deprecated);
            recordName = id.getText();
        }
    ;

superClassClause! returns [String superclass]
{
    superclass = null;
}
    :
        ( EXTENDS superclass=classOrInterfaceType )?
    ;

// Definition of a Java Interface
interfaceDefinition! [Modifiers mods] returns [String name]
{
    boolean deprecated = false;
    CloverToken endOfBlock = null;
    ClassEntryNode classEntry = null;
    name = null;
    String typeParam = null;
}
    :
        INTERFACE
        {
            deprecated = maybeEnterDeprecated(lt(0));
        }
        id:IDENT
        // it _might_ have type parameters
        (typeParam=typeParameters)?
        {
            classEntry = enterClass(mods, ct(id), true, false, false);
        }
        // it might extend some other interfaces
        interfaceExtends

        // if an interface is sealed then it must have the permits clause
        permitsClause[mods]

        // now parse the body of the interface (looks like a class...)
        endOfBlock = classBlock[classEntry]
        {
            exitClass(endOfBlock, classEntry);
            maybeExitDeprecated(deprecated);
            name = id.getText();
        }
    ;

enumDefinition! [Modifiers mods] returns [String name]
{
    boolean deprecated = false;
    CloverToken endOfBlock = null;
    ClassEntryNode classEntry = null;
    name = null;
}
    :
        ENUM
        {
            deprecated = maybeEnterDeprecated(lt(0));
        }

        id:IDENT

        {
            classEntry = enterClass(mods, ct(id), false, true, false);
        }

        implementsClause
        endOfBlock = enumBlock[classEntry]
        {
            exitClass(endOfBlock, classEntry);
            maybeExitDeprecated(deprecated);
            name = id.getText();
        }
    ;

annotationTypeDeclaration [Modifiers mods] returns [String name]
{
    CloverToken endOfBlock = null;
    ClassEntryNode classEntry = null;
    name = null;
}
    :
        AT INTERFACE id:IDENT
        {
            classEntry = enterClass(mods, ct(id), false, false, true);
        }
        endOfBlock = annotationTypeBody[classEntry]
        {
            exitClass(endOfBlock, classEntry);
            name = id.getText();
        }
    ;

typeParameters returns [String asString]
{
    int currentLtLevel = 0;
    CloverToken start = lt(1);
    asString = null;
}
    :
        {
            currentLtLevel = ltCounter;
        }
        LT { ltCounter++; }
        typeParameter (COMMA typeParameter)*
        (typeArgumentsEnd)?
        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?

        {
            asString = TokenListUtil.getNormalisedSequence(start, lt(0));
        }
    ;

typeParameter
{
    String type = null;
    AnnotationImpl ann = null;
}
    :
        (ann=annotation)*
        (
            IDENT
        |
            QUESTION
        )
        (
            // I'm pretty sure Antlr generates the right thing here:
            options{generateAmbigWarnings=false;}:
            EXTENDS type=classOrInterfaceType
            (BAND type=classOrInterfaceType)*
        )?
    ;

// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
classBlock [ClassEntryNode classEntry] returns [CloverToken t]
{
    t = null;
}
    :
        ip:LCURLY!
        {
            setRecorderMemberInsertPoint(classEntry, ct(ip));
        }
        (
            field[classEntry]
        |
            SEMI!
        )*
        rc:RCURLY!
        {
            t = ct(rc);
        }
    ;

enumBlock [ClassEntryNode classEntry] returns [CloverToken t]
{
    boolean topLevelSave = topLevelClass;
    t = null;
}
    :   LCURLY!
        (
            options { warnWhenFollowAmbig = false; }:
            (
                enumConstant
                (
                    // CONFLICT: does a COMMA after an enumConstant start a new
                    //           constant or start the optional ',' at end?
                    //           ANTLR generates proper code by matching
                    //           the comma as soon as possible.
                    options { warnWhenFollowAmbig = false; }:
                    COMMA enumConstant
                )*
            )?
            (COMMA!)?

            (
                {
                    topLevelSave = topLevelClass;
                    topLevelClass = false;
                }
                //TODO what about the case where an enum constant declares a body
                // but there is no SEMI... where to we put the inst var?
                SEMI
                (
                    field[null]
                |
                    SEMI!
                )*
                {
                    topLevelClass = topLevelSave;
                }
            )?
        )?
        ip:RCURLY!
        {
            t = ct(ip);
            setRecorderMemberInsertPoint(classEntry, t);
        }
    ;

enumConstant
{
    boolean topLevelSave = topLevelClass;
    CloverToken endOfBlock = null;
    AnnotationImpl ann = null;
}
    :
        {
           topLevelSave = topLevelClass;
           topLevelClass = false;
        }
        (ann=annotation)*
        IDENT ( LPAREN argList RPAREN )?
        (
            endOfBlock = classBlock[null]
        )?
        {
            topLevelClass = topLevelSave;
        }
    ;

annotationTypeBody [ClassEntryNode classEntry] returns [CloverToken t]
{
    boolean topLevelSave = topLevelClass;
    t = null;
    Modifiers mods = null;
    String type = null;
}
    :
        ip:LCURLY!
        {
            setRecorderMemberInsertPoint(classEntry, ct(ip));
        }
        (
            // an annotation member ("method")
            // disambiguation: lookup further up to parenthesis, e.g. "public int foo("
            ( methodModifiers[false] typeSpec IDENT LPAREN ) =>

            mods=methodModifiers[false]
            type=typeSpec
            IDENT LPAREN RPAREN (annDefaultValue)?  SEMI

        |
            // a constant declaration
            // disambiguation: lookup further up to a first variable, e.g. "public int x = 8"
            ( fieldModifiers[false] typeSpec variableDeclarator ) =>

            mods=fieldModifiers[false]
            type=typeSpec
            variableDefinitions SEMI

        |
            // a nested type declaration
            // disambiguation: lookup further up to "class/interface" keyword, e.g. "public final class"
            ( classOrInterfaceModifiers[false] ( CLASS | INTERFACE | AT INTERFACE | ENUM | { isCurrentKeyword("record") }? ) ) =>

            {
                topLevelSave = topLevelClass;
                topLevelClass = false;
            }
            mods=classOrInterfaceModifiers[false]
            typeDefinition2[mods, null, true]
            {
                topLevelClass = topLevelSave;
            }
        |
            // a semicolon
            SEMI
        )*
        endOfBlock:RCURLY!
        {
            t = ct(endOfBlock);
        }
    ;

protected
annDefaultValue
{
    boolean ft = false;
}
    :
        DEFAULT ft=annMemberValue[false,false]
    ;

// An interface can extend several other interfaces...
interfaceExtends
{
    String type = null;
}
    :
        (
            EXTENDS!
            type=classOrInterfaceType ( COMMA! type=classOrInterfaceType )*
        )?
    ;

// A class can implement several interfaces...
implementsClause
{
    String type = null;
}
    :
        (
            IMPLEMENTS! type=classOrInterfaceType ( COMMA! type=classOrInterfaceType )*
        )?
    ;

// for a sealed class or interface
permitsClause [Modifiers mods]
{
    String type = null;
}
    :
        (
            {
                (mods.getMask() & com.atlassian.clover.registry.entities.ModifierExt.SEALED) != 0
                    && isNextKeyword("permits")
            }?
            IDENT!
            type=classOrInterfaceType ( COMMA! type=classOrInterfaceType )*
        )?
    ;

// Now the various things that can be defined inside a class or interface...
// Note that not all of these are really valid in an interface (constructors,
//   for example), and if this grammar were used for a compiler there would
//   need to be some semantic checks to make sure we're doing the right thing...
field! [ClassEntryNode containingClass]
{
    CloverToken tmp;
    CloverToken first = lt(1);
    CloverToken endSig = null;
    String typeParam = null;
    boolean deprecated = false;
    Modifiers mods = null;
    String name = null;
    MethodSignature signature = null;
    MethodSignatureExt signatureExt = null;
    String [] throwsTypes = null;
    String returnType = "";
    String brackets = "";
    Parameter [] parameters = null;
    Map<String, List<String>> tags = null;
    String typename = null;
}
    :
    // read javadocs written before the member (if any)
    {
        tags = TokenListUtil.getJDocTagsAndValuesOnBlock(first);
    }
    (

        // compact canonical constructors
        // lookup for e.g. "@Anno public <T> Rec {"
        ( compactConstructorSignature[tags, first, true] LCURLY ) =>
        signatureExt = compactConstructorSignature[tags, first, false]
        ccb:constructorBody[signatureExt.signature(), first, signatureExt.endToken()]
        { maybeExitDeprecated(signatureExt.isDeprecated()); }

    |

        // standard constructors
        // lookup for e.g. "@Anno public <T> Rec(int a, int b) throws Exception {"
        ( standardConstructorSignature[tags, first, true] LCURLY ) =>
        signatureExt = standardConstructorSignature[tags, first, false]
        cb:constructorBody[signatureExt.signature(), first, signatureExt.endToken()]
        { maybeExitDeprecated(signatureExt.isDeprecated()); }

    |
        // FIELDS
        // look further to recognize that it's a field
        ( fieldModifiers[false] typeSpec variableDefinitions ) =>

        mods=fieldModifiers[false]
        { deprecated = maybeEnterDeprecated(tags, mods); }
        returnType=typeSpec
        variableDefinitions
        SEMI
        { maybeExitDeprecated(deprecated); }

    |

        // "static { ... }" class initializer
        {
            deprecated = maybeEnterDeprecated(first);
            signature = new MethodSignature(null, null, null, "<clinit>, line " + first.getLine(), null, null, null, null);
        }
        STATIC
        outerCompoundStmt[signature, first, null, ContextStore.CONTEXT_STATIC]
        { maybeExitDeprecated(deprecated); }

    |

        // ...or "{ ... }" instance initializer
        {
            deprecated = maybeEnterDeprecated(first);
            signature = new MethodSignature(null, null, null, "<init>, line " + first.getLine(), null, null, null, null);
        }
        outerCompoundStmt[signature, first, null, ContextStore.CONTEXT_INSTANCE]
        { maybeExitDeprecated(deprecated); }

    |

        // INNER CLASSES, INTERFACES, ENUMS, ANNOTATIONS, RECORDS
        // look further to recognize that it's a definition of an inner type
        ( classOrInterfaceModifiers[false] ( CLASS | INTERFACE | AT INTERFACE | ENUM | { isCurrentKeyword("record") }? ) IDENT ) =>

        mods=classOrInterfaceModifiers[false]
        { deprecated = maybeEnterDeprecated(tags, mods); }
        (
            typename = classDefinition[mods]       // inner class
        |
            typename = recordDefinition[mods]      // inner record
        |
            typename = interfaceDefinition[mods]   // inner interface
        |
            typename = enumDefinition[mods]   // inner enum
        |
            typename = annotationTypeDeclaration[mods] // inner annotation decl
        )
        { maybeExitDeprecated(deprecated); }

    |

        // methods
        // lookup for e.g. "@Anno public <S, T> S foo(T in) throws Exception {"
        // or abstract methods ending with a semicolon, e.g. "abstract void call();"
        ( methodSignature[tags, first, true] (LCURLY|SEMI) ) =>
        signatureExt = methodSignature[tags, first, false]
        (
            outerCompoundStmt[signatureExt.signature(), first, signatureExt.endToken(), ContextStore.CONTEXT_METHOD]
        |
            SEMI
        )
        { maybeExitDeprecated(signatureExt.isDeprecated()); }

    )
    ;

constructorBody[MethodSignature signature, CloverToken start, CloverToken endSig]
{
    CloverToken endOfInv = null;
    CloverToken tmp;
}
    :
        {
            enterContext(ContextStore.CONTEXT_CTOR);
        }
        lc:LCURLY
        // Predicate might be slow but only checked once per constructor def
        // not for general methods.
        (
            (explicitConstructorInvocation) =>
            endOfInv = explicitConstructorInvocation
        |
            /* empty */
        )
        (tmp=statement[null])*
        rc:RCURLY!

        {
            // special case for instrumenting entry to ctors - HACK add ctor sig for completeness
            MethodEntryInstrEmitter entry = instrEnterMethod(signature, start, ct(lc), endOfInv);
            instrExitMethod(entry, ct(rc));
            exitContext();
            fileInfo.addMethodMarker(entry, start, endSig, ct(rc));
        }
    ;

explicitConstructorInvocation returns [CloverToken t]
{
    t = null;
}
    :
        (
            options {
                // this/super can begin a primaryExpressionPart too; with finite
                // lookahead ANTLR will think the 3rd alternative conflicts
                // with 1, 2.  I am shutting off warning since ANTLR resolves
                // the nondeterminism by correctly matching alts 1 or 2 when
                // it sees this( or super(
                generateAmbigWarnings=false;
            }:

            pos1:THIS! LPAREN argList RPAREN! t1:SEMI!
            {
                t=instrInlineAfter(ct(t1), ct(pos1), ct(t1));
            }

        |
            pos2:SUPER! lp2:LPAREN^ argList RPAREN! t2:SEMI!
            {
                t=instrInlineAfter(ct(t2), ct(pos2), ct(t2));
            }

        |
            // (new Outer()).super()  (create enclosing instance)
            primaryExpressionPart
            (DOT! THIS)? // HACK see CCD-264 - explicit ctor invocation can have form ClassName.this.super(..)
            DOT! pos3:SUPER! lp3:LPAREN^ argList RPAREN! t3:SEMI!
            {
                t=instrInlineAfter(ct(t3), ct(pos3), ct(t3));
            }
        )
    ;

variableDefinitions
    :
        variableDeclarator (COMMA! variableDeclarator)*
    ;

/** Declaration of a variable.  This can be a class/instance variable,
 *   or a local variable in a method
 * It can also include possible initialization.
 */
variableDeclarator!
{
    String brackets = null;
}
    :
        IDENT brackets=declaratorBrackets varInitializer
    ;

declaratorBrackets returns [String brackets]
{
    brackets = "";
    AnnotationImpl ann = null;
}
    :
        (
            (ann=annotation)*
            LBRACK RBRACK! { brackets += "[]"; }
        )*
    ;

varInitializer
    :
        ( ASSIGN initializer )?
    ;

// This is an initializer used to set up an array.
arrayInitializer
    :
        LCURLY
        (
            initializer
            (
                // CONFLICT: does a COMMA after an initializer start a new
                //           initializer or start the option ',' at end?
                //           ANTLR generates proper code by matching
                //           the comma as soon as possible.
                options { warnWhenFollowAmbig = false; }:
                COMMA! initializer
            )*

        )?
        (COMMA!)?
        RCURLY!
    ;


// The two "things" that can initialize an array element are an expression
//   and another (nested) array initializer.
initializer
    :
        expression
    |
        arrayInitializer
    ;

// This is a list of exception classes that the method is declared to throw
throwsClause returns [String [] throwsTypes]
{
    List<String> throwsList = new ArrayList<String>();
    throwsTypes = null;
    String id;
    AnnotationImpl ann = null;
}
    :
        THROWS (ann=annotation)* id=identifier
        {
            throwsList.add(id);
        }
        (
            COMMA! (ann=annotation)* id=identifier
            {
                throwsList.add(id);
            }
        )*
        {
            throwsTypes = (String[])throwsList.toArray(new String[throwsList.size()]);
        }
    ;


// A list of formal parameters
parameterDeclarationList returns [Parameter [] params]
{
    List<Parameter> parameters = new ArrayList<Parameter>();
    Parameter param = null;
    params = new Parameter[0];
}
    :
        (
            param=parameterDeclaration
            {
                parameters.add(param);
            }
            (
                COMMA! param=parameterDeclaration
                {
                    parameters.add(param);
                }
            )*
        )?
        {
            params = (Parameter[])parameters.toArray(new Parameter [parameters.size()]);
        }
    ;

// A formal parameter.
parameterDeclaration! returns [Parameter parameter]
{
    Parameter param = null;
    String brackets = "";
    String type=null;
    parameter=null;
}
    :
        parameterModifier type=typeSpec (ELLIPSIS)? i:IDENT
        brackets=declaratorBrackets
        {
            parameter = new Parameter(type+brackets,i.getText());
        }
    ;

parameterModifier
{
    AnnotationImpl ann = null;
}
    :
        (
            options { greedy=true; }:
            ann=annotation
        )*
        (
            f:FINAL
            (
                options { greedy=true; }:
                ann=annotation
            )*
        )?
    ;


/**
 * A list of declared parameters (one or more) with unspecified types, used in argument list of a lambda function,
 * for example:
 *   <code>x, y, z</code>
 */
implicitParameterDeclarationList returns [Parameter[] parameters]
{
    List<Parameter> parameterList = new ArrayList<Parameter>();
    Parameter param = null;
    parameters = new Parameter[0];
}
    :
        param=implicitParameterDeclaration
        {
            parameterList.add(param);
        }
        (
            COMMA! param=implicitParameterDeclaration
            {
                parameterList.add(param);
            }
        )*
        {
            parameters = parameterList.toArray(new Parameter[parameterList.size()]);
        }
    ;

/**
 * A parameter with an undefined type, used in lambda function argument list
 */
implicitParameterDeclaration returns [Parameter parameter]
{
    parameter = null;
}
    :
        i:IDENT
        {
            parameter = new Parameter(Parameter.INFERRED, i.getText());
        }
    ;


// Compound statement.  This is used in many contexts:
//   Inside a class definition prefixed with "static":
//      it is a class initializer
//   Inside a class definition without "static":
//      it is an instance initializer
//   As the body of a method
//   As a completely independent braced block of code inside a method
//      it starts a new scope for variable definitions
compoundStatement returns [CloverToken t]
{
    t = null;
}
    :
        LCURLY
        // include the (possibly-empty) list of statements
        (t=statement[null])*
        rc:RCURLY!
        {
            t = ct(rc);
        }
    ;

outerCompoundStmt [MethodSignature sig, CloverToken start, CloverToken endSig, int context]
{
    CloverToken tmp;
}
    :
        {
            enterContext(context);
        }
        lc:LCURLY
        // include the (possibly-empty) list of statements
        (tmp=statement[null])*
        rc:RCURLY!
        {
            MethodEntryInstrEmitter entry = instrEnterMethod(sig, start, ct(lc));
            instrExitMethod(entry, ct(rc));
            exitContext();
            if (context == ContextStore.CONTEXT_METHOD) {
                fileInfo.addMethodMarker(entry, start, endSig, ct(rc));
            }
        }
    ;


// owningLabel is the ident token of the label for this statement, or null if this
// statement has no label
// returns the last token of the matched statement
statement [CloverToken owningLabel] returns [CloverToken last]
{
    CloverToken first = null;
    CloverToken tmp = null;
    CloverToken labelTok = null;
    boolean assertColonPart = false;
    last = null;
    boolean labelled = (owningLabel != null);
    FlagDeclEmitter flag = null;
    boolean matchable = true;
    boolean instrumentable = !labelled;
    CloverToken instr = null; // instr point for statement
    CloverToken flushAfter = null; // if not null, add maybeFlush instr
    int complexity=  0;
    int tmpCmp = 0;
    boolean wasDefault = false;
    Modifiers mods = null;
    Parameter parameter = null;
    String classname = null;
    ContextSet saveContext = getCurrentContext();
    ContextSetAndComplexity contextAndComplexity = null;
}
    :
    {
        first = lt(1);
    }
    (
        // an assert statement
        ASSERT
        {
            enterContext(ContextStore.CONTEXT_ASSERT);
            instrumentable = false;
            saveContext = getCurrentContext();
        }
        {
            tmp=lt(1);
        }
        expression
        ( colon:COLON! {instrBoolExpr(tmp, ct(colon)); assertColonPart=true;}  expression )?
        semi:SEMI!
        {
            if (!assertColonPart) {
                 instrBoolExpr(tmp, ct(semi));
            }
            exitContext();
        }

    |
        // A list of statements in curly braces -- start a new scope!
        tmp = compoundStatement
        {
            matchable = false;
            instrumentable = false;
        }

    |
        // declarations are ambiguous with "ID DOT" relative to expression
        // statements.  Must backtrack to be sure.  Could use a semantic
        // predicate to test symbol table to see what the type was coming
        // up, but that's pretty hard without a symbol table ;)
        (declaration) =>
        declaration se1:SEMI!
        {
            flushAfter = ct(se1);
        }

    |
        // note: yield can appear inside traditional case blocks and also in compound statement blocks inside
        // lambda case; that's why rule is placed here; a drawback is that instrumenter accepts yield in any place
        ( { isNextKeyword("yield") }? IDENT expression SEMI ) =>
        { isNextKeyword("yield") }? IDENT expression SEMI!

    |
        // NOTE: we check for records before normal statement as "record" can be recognized as IDENT leading to syntax error
        // record definition
        (classOrInterfaceModifiers[false] { isCurrentKeyword("record") }? ) =>
        mods=classOrInterfaceModifiers[false]! classname=recordDefinition[mods]
        {
            instrumentable = false;
            //TODO - return last token
        }

    |
        // class definition
        (classOrInterfaceModifiers[false] CLASS) =>
        mods=classOrInterfaceModifiers[false]! classname=classDefinition[mods]
        {
            instrumentable = false;
            //TODO - return last token
        }

    |
        // An expression statement.  This could be a method call,
        // assignment statement, or any other expression evaluated for
        // side-effects.
        ( expression SEMI ) =>
        expression se2:SEMI!
        {
            flushAfter = ct(se2);
        }

    |
        // Attach a label to the front of a statement
        IDENT COLON
        {
            labelTok = owningLabel;
            if (!labelled) {
                labelTok = first;
            }
        }
        last=statement[labelTok]

    |
        // If-else statement
        IF
        {
            enterContext(ContextStore.CONTEXT_IF);
            saveContext = getCurrentContext();
        }
        LPAREN!
        {
            tmp=lt(1);
        }
        expression
        rp1:RPAREN!
        {
            instrBoolExpr(tmp, ct(rp1));
            addOpenBraceAfter(ct(rp1));
        }
        last=statement[null]
        {
            addCloseBraceAfter(last);
            exitContext();
        }
        (
            // CONFLICT: the old "dangling-else" problem...
            //           ANTLR generates proper code matching
            //           as soon as possible.  Hush warning.
            options {
                warnWhenFollowAmbig = false;
            }:
            el:ELSE!
            {
                addOpenBraceAfter(ct(el));
                enterContext(ContextStore.CONTEXT_ELSE);
                saveContext = getCurrentContext();
            }
            last = statement[null]
            {
                addCloseBraceAfter(last);
                exitContext();
            }
        )?

    |
        // For statement
        FOR
        {
            enterContext(ContextStore.CONTEXT_FOR);
            saveContext = getCurrentContext();
        }
        LPAREN!
        (
            (parameterDeclaration COLON) =>
            (
                // enhanced for
                parameter=parameterDeclaration COLON expression
            )
        |
            (
                // traditional for
                forInit SEMI!   // initializer
                forCond    // condition test
                forIter         // updater
            )
        )
        rp:RPAREN!
        {
            addOpenBraceAfter(ct(rp));
        }
        last = statement[null]   // statement to loop over
        {
            addCloseBraceAfter(last);
            exitContext();
        }

    |
        // While statement
        WHILE
        {
            enterContext(ContextStore.CONTEXT_WHILE);
            saveContext = getCurrentContext();
        }
        LPAREN!
        {
            tmp = lt(1);
        }
        expression
        rp2:RPAREN!
        {
            instrBoolExpr(tmp, ct(rp2));
            addOpenBraceAfter(ct(rp2));
        }
        last=statement[null]
        {
            addCloseBraceAfter(last);
            exitContext();
        }

    |
        // do-while statement
        d1:DO
        {
            addOpenBraceAfter(ct(d1));
            enterContext(ContextStore.CONTEXT_DO);
            saveContext = getCurrentContext();
        }
        tmp=statement[null]
        {
            addCloseBraceAfter(tmp);
            exitContext();
        }
        WHILE! LPAREN!
        {
            tmp=lt(1);
        }
        expression rp3:RPAREN!
        {
            instrBoolExpr(tmp, ct(rp3));
        }
        sem:SEMI!
        {
            flushAfter = ct(sem);
        }

    |
        // get out of a loop (or switch)
        BREAK (IDENT)? SEMI!

    |
        // do next iteration of a loop
        CONTINUE (IDENT)? SEMI!

    |
        // Return an expression
        RETURN (expression)? SEMI!

    |
        // a classic switch/case with colons
        ( SWITCH LPAREN expression RPAREN LCURLY (CASE expression | DEFAULT) COLON) =>
        contextAndComplexity = colonSwitchExpression[owningLabel, false]
        {
            saveContext = contextAndComplexity.context;
            complexity += contextAndComplexity.complexity;
        }
    |
        // a new switch/case with lambdas
        ( SWITCH LPAREN expression RPAREN LCURLY (CASE patternMatch | DEFAULT) LAMBDA) =>
        contextAndComplexity = lambdaSwitchExpression[owningLabel]
        {
            saveContext = contextAndComplexity.context;
            complexity += contextAndComplexity.complexity;
        }
    |
        // exception try-catch block
        (tryCatchBlock[labelled]) =>
        last = tryCatchBlock[labelled]
        {
            instrumentable = false; /* instrumentation happens in the tryCatchBlock rule */
        }

    |
        // throw an exception
        THROW expression SEMI!

    |
        // synchronize a statement
        SYNCHRONIZED LPAREN! expression RPAREN!
        {
            enterContext(ContextStore.CONTEXT_SYNC);
            saveContext = getCurrentContext();
        }
        last = compoundStatement
        {
            exitContext();
        }


    |
        // empty statement
        SEMI
    )
        {
            if (last == null) {
                last = lt(0);
            }

            if (instrumentable) {
                instrInlineBefore(first, last, saveContext, complexity);
            }

            if (flushAfter != null) {
                maybeAddFlushInstr(flushAfter);
            }

            if (matchable) {
                fileInfo.addStatementMarker(first, last);
            }
        }
    ;

/**
 * A group of one or more "case x:" labels, followed by a list of statements.
 */
colonCasesGroup[FlagDeclEmitter flag] returns [int complexity]
{
    int tmp = 0;
    complexity = 0;
}
    :
        (
            // CONFLICT: to which case group do the statements bind?
            //           ANTLR generates proper code: it groups the
            //           many "case"/"default" labels together then
            //           follows them with the statements
            options {
                warnWhenFollowAmbig = false;
            }:
            tmp = colonCase[flag]
            {
                complexity += tmp;
            }
        )+
        caseStatementsList
    ;

/**
 * A single "case x:" or "default:" label.
 */
colonCase[FlagDeclEmitter flag] returns [int complexity]
{
    Token pos = null;
    complexity = 0;
}
    :
        (
            si1:CASE
            {
                constExpr = true;
            }
            expression
            {
                constExpr = false;
                pos = si1;
                complexity++;
            }
        |
            si2:DEFAULT
            {
                pos = si2;
            }
        )
        t:COLON!
        {
            if (flag != null) {
                instrInlineAfter(ct(t), ct(pos), ct(t), flag);
            } else {
                instrInlineAfter(ct(t), ct(pos), ct(t));
            }
            fileInfo.setSuppressFallthroughWarnings(true);
        }
    ;

/**
 * A list of statements inside a single "case" or "default" block.
 */
caseStatementsList
{
    CloverToken tmp;
}
    :
        (tmp=statement[null])*
    ;

// The initializer for a for loop
forInit
    :
        (
            // if it looks like a declaration, it is
            (declaration) =>
            declaration
        |
            // otherwise it could be an expression list...
            expressionList
        )?
    ;

forCond
{
    CloverToken tmp = null;
}
    :
        (
            {
                tmp=lt(1);
            }
            expression se:SEMI!
            {
                instrBoolExpr(tmp, ct(se));
            }
        )
        |
            SEMI!
    ;

forIter
    :
        (expressionList)?
    ;

// an exception handler try/catch block
tryCatchBlock [boolean labelled] returns [CloverToken last]
{
    last = null;
    int complexity = 0;
    ContextSet saveContext = getCurrentContext();
}
    :
        tr:TRY
        (
            lp:LPAREN
            {
                insertAutoCloseableClassDecl(ct(tr));
            }
            (
                (IDENT) =>
                variableDeclarator
            |
                declaration
            )
            {
                complexity++;
                instrArmDecl((ct(lp)).getNext(), lt(0), saveContext);
            }
            (
                semi:SEMI
                ( (IDENT) => variableDeclarator | declaration )
                {
                    complexity++;
                    instrArmDecl((ct(semi)).getNext(), lt(0), saveContext);
                }
            )*
            (SEMI)?
            rp:RPAREN
        )?
        {
            enterContext(ContextStore.CONTEXT_TRY);
            saveContext = getCurrentContext();
        }
        last=compoundStatement
        {
            exitContext();
        }
        (
            last=handler
            {
                complexity++;
            }
        )*
        (
            FINALLY
            {
                enterContext(ContextStore.CONTEXT_FINALLY); saveContext = getCurrentContext();
            }
            last=compoundStatement
            {
                exitContext();
            }
        )?
        {
            if (!labelled) {
                instrInlineBefore(ct(tr), last, saveContext, complexity);
            }
        }
    ;

// an exception handler
handler returns [CloverToken last]
{
    AnnotationImpl an;
    String ts;
    last = null;
}
    :
        CATCH
        LPAREN!
        (
            options { greedy=true; }:
            an=annotation2[false]
        )*
        (FINAL)?
        (
            options { greedy=true; }:
            an=annotation2[false]
        )*
        ts=typeSpec
        (BOR ts=typeSpec)*
        IDENT
        RPAREN!
        {
            enterContext(ContextStore.CONTEXT_CATCH);
        }
        last=compoundStatement
        {
            exitContext();
        }
    ;

/////////////////////////////////////////////////
// LAMBDAS
/////////////////////////////////////////////////

/**
 * A predicate which recognizes whether we have a lambda function or not. Predicate tries to parse tokens
 * up to the LAMBDA symbol.
 */
lambdaFunctionPredicate
{
    Parameter[] la;
    String type, typeExt;
}
    :
        // lambda can have an optional class cast, e.g: "Object o = (Runnable) () -> { ... };"
        // therefore we must use semantic predicate and look forward up to "->" symbol in order to clarify whether
        // we have "(cast) (args) ->" or "(args) ->"; otherwise ANTLR could treat a single-argument list
        // like "(x)" as a type cast and fail on parsing futher symbols
        // type cast can also contain interfaces joined by "&" operator, e.g. "(Runnable & Serializable) () ->"

        (LPAREN classTypeSpec (BAND classOrInterfaceType)* RPAREN la=lambdaArgs LAMBDA) =>
        LPAREN type=classTypeSpec (BAND typeExt=classOrInterfaceType)* RPAREN la=lambdaArgs LAMBDA
    |
        (la=lambdaArgs LAMBDA)=>
        la=lambdaArgs LAMBDA
    ;

/**
 * Lambda function in a form like:
 *   <code>(arguments) -> body</code>
 * <p/>
 * where body is in a form of a single statement or expression, e.g.:
 *   <code>System.out.println("Hello")</code>
 *   <code>x + y</code>
 * <p/>
 * or in a form of the code block, e.g.:
 *   <pre>
 *   {
 *      the;
 *      code;
 *      block;
 *   }
 *   </pre>
 * <p/>
 * or in a form of the method reference, e.g:
 *   <code>Math::abs</code>
 */
lambdaFunction returns [CloverToken last]
{
    CloverToken startLambdaArgs = null; // remembers where lambda's argument list begins
    CloverToken startLambdaBody = null; // remembers where the expression begins
    CloverToken cs = null; // unused
    Parameter[] la = null; // list of lambda's type arguments
    String type;           // optional class cast
    String typeExt;        // .. with extra interfaces concatenated by &
    MethodSignature lambdaSignature = null;
    LambdaExpressionEntryEmitter expressionEntryEmitter = null;
    LambdaBlockEntryEmitter blockEntryEmitter = null;
    LambdaExprToBlockStartEntryEmitter exprToBlockStartEntryEmitter = null;
    boolean exprToBlockHeuristic = false;

    last = null;

    /*
     * A pair of marker tokens used to remember a class cast. This pair of markers is used to solve type-inference for
     * lambdaInc wrapper. For example:
     * <pre>
     *   Object o = (Runnable) () -> System.out.println("lambda expression with class cast");
     * </pre>
     * will be wrapped into lambdaInc like this:
     * <pre>
     *   Object o = (Runnable) lambdaInc(123, () -> System.out.println("lambda expression with class cast"));
     * </pre>
     * The problem is that javac is unable to infer proper type of lambdaInc - it sees Object.
     *
     * In such case when Clover finds a lambda with a class cast, it will memorize class cast start/end and copy
     * it into a lambda wrapper:
     * <pre>
     *   Object o = (Runnable) lambdaInc(123, (Runnable)() -> System.out.println("lambda expression with class cast"));
     *         start^     end^                ^^^copy^^^
     * </pre>
     */
    CloverToken classCastStart = null;
    CloverToken classCastEnd = null;
}
    :
        // optional class cast, e.g: "Object o = (Runnable) () -> { ... };" therefore we can have
        // "(cast) (args) ->" or "(args) ->"; see lambdaFunctionPredicate
        (
            (LPAREN type=classTypeSpec (BAND classOrInterfaceType)* RPAREN la=lambdaArgs LAMBDA) =>
                { classCastStart = lt(1); }
                LPAREN type=classTypeSpec
                (BAND typeExt=classOrInterfaceType { type += "&" + typeExt; } )*
                RPAREN
                { classCastEnd = lt(0); }
                { startLambdaArgs = lt(1); }
                la=lambdaArgs
                LAMBDA
        |
            (la=lambdaArgs LAMBDA) =>
                { startLambdaArgs = lt(1); }
                la=lambdaArgs
                LAMBDA
        )
        {
            lambdaSignature = new MethodSignature(LambdaUtil.generateLambdaNameWithId(la, lambdaCounter++),
                    null, null, la, null, new Modifiers());
        }
        // body is either a single expression or a block
        // note: the non-expression statement (for, while, try etc) is not allowed by javac as a single-line lambda
        // either an expression or a block statement must be used
        (
            {
                // wrap entire lambda, including argument list and not just an expression body
                // pass also a preceding class cast if present (non-null)
                startLambdaBody = lt(1);
                exprToBlockHeuristic = RewriteLambdaToBlockMatcher.shouldRewriteAsBlock(identifiersStack);
                if (exprToBlockHeuristic) {
                    exprToBlockStartEntryEmitter = instrEnterLambdaExprToBlockExpression(lambdaSignature, startLambdaArgs, startLambdaBody);
                } else {
                    expressionEntryEmitter = instrEnterLambdaExpression(lambdaSignature, startLambdaArgs, startLambdaBody,
                            classCastStart, classCastEnd);
                }
            }
            expression
            {   if (exprToBlockHeuristic) {
                    instrExitLambdaExprToBlockExpression(exprToBlockStartEntryEmitter, lt(0));
                } else {
                    instrExitLambdaExpression(expressionEntryEmitter, lt(0));
                }
            }
        |
            {
                // expected LT(1)=LCURLY
                blockEntryEmitter = instrEnterLambdaBlock(lambdaSignature, lt(1));
            }
            cs=compoundStatement
            {
                // expected LT(0)=RCURLY
                instrExitLambdaBlock(blockEntryEmitter, lt(0));
            }
        )
    ;

/**
 * List of formal arguments for lambda function definition. Possible forms are:
 * 1) Empty list of arguments for a lambda function, i.e.:
 *     <code>()</code>
 * <p/>
 * 2) List of explicitly declared arguments' types, for instance:
 *     <code>(Integer x, String s)</code>
 * <p/>
 * 3) List of arguments for where types are undefined, for example:
 *     <pre>
 *         (x, y, z)    // few identifiers separated by comma, enclosed in parentheses
 *         (abc)        // one identifier, enclosed in parentheses
 *         i            // exactly one identifier, without parentheses
 *     </pre>
 */
lambdaArgs returns [Parameter[] parameters]
{
    Parameter p;
    Parameter[] ps;
    parameters = new Parameter[0]; // empty list by default
}
    :
        (
            LPAREN
            (
                // explicitly declared types of arguments (0 or more arguments, so it handles also the empty list case)
                ps=parameterDeclarationList { parameters = ps; }
            |
                // undefined types of arguments (1 or more arguments)
                ps=implicitParameterDeclarationList { parameters = ps; }
            )
            RPAREN
        |
            // an exactly one argument of undefined type, not enclosed in "( )"
            p=implicitParameterDeclaration
            {
                parameters = new Parameter[1];
                parameters[0] = p;
            }
        )
    ;


/////////////////////////////////////////////////
// expressions
/////////////////////////////////////////////////

// Note that most of these expressions follow the pattern
//   thisLevelExpression :
//       nextHigherPrecedenceExpression
//           (OPERATOR nextHigherPrecedenceExpression)*
// which is a standard recursive definition for a parsing an expression.
// The operators in java have the following precedences:
//    lowest  (13)  = *= /= %= += -= <<= >>= >>>= &= ^= |=
//            (12)  ?:
//            (11)  ||
//            (10)  &&
//            ( 9)  |
//            ( 8)  ^
//            ( 7)  &
//            ( 6)  == !=
//            ( 5)  < <= > >=
//            ( 4)  << >>
//            ( 3)  +(binary) -(binary)
//            ( 2)  * / %
//            ( 1)  ++ -- +(unary) -(unary)  ~  !  (type)
//                  []   () (method call)  . (dot -- identifier qualification)
//                  new   ()  (explicit parenthesis)
//
// the last two are not usually on a precedence chart; I put them in
// to point out that new has a higher precedence than '.', so you
// can validy use
//     new Frame().show()
//
// Note that the above precedence levels map to the rules below...
// Once you have a precedence chart, writing the appropriate rules as below
//   is usually very straightfoward



// the mother of all expressions
expression
    :
    {
        pushHeadIdentifierStack();
    }
    assignmentExpression
    {
        popHeadIdentifierStack();
    }
    ;


// This is a list of expressions.
expressionList
    :   expression (COMMA! expression)*
    ;


// assignment expression (level 13)
assignmentExpression
    :
        conditionalExpression
        ( options { greedy=true; }:
            (   ASSIGN^
            |   PLUS_ASSIGN^
            |   MINUS_ASSIGN^
            |   STAR_ASSIGN^
            |   DIV_ASSIGN^
            |   MOD_ASSIGN^
            |   SR_ASSIGN^
            |   BSR_ASSIGN^
            |   SL_ASSIGN^
            |   BAND_ASSIGN^
            |   BXOR_ASSIGN^
            |   BOR_ASSIGN^
            )
            assignmentExpression
        )?
    ;


// conditional test (level 12)
conditionalExpression
{
    CloverToken startOfCond = null;
    CloverToken lf = null;
}
    :
        (lambdaFunctionPredicate) => lf=lambdaFunction
    |
        (logicalOrExpression (QUESTION)?) =>
        {startOfCond = lt(1);}
        logicalOrExpression
        (   endOfCond:QUESTION
            {if (!constExpr) instrBoolExpr(startOfCond, ct(endOfCond));}
            assignmentExpression COLON! conditionalExpression
        )?
    ;


// logical or (||)  (level 11)
logicalOrExpression
    :   logicalAndExpression (LOR logicalAndExpression)*
    ;


// logical and (&&)  (level 10)
logicalAndExpression
    :   inclusiveOrExpression (LAND inclusiveOrExpression)*
    ;


// bitwise or non-short-circuiting or (|)  (level 9)
inclusiveOrExpression
    :   exclusiveOrExpression (BOR exclusiveOrExpression)*
    ;


// exclusive or (^)  (level 8)
exclusiveOrExpression
    :   andExpression (BXOR andExpression)*
    ;


// bitwise or non-short-circuiting and (&)  (level 7)
andExpression
    :   equalityExpression (BAND equalityExpression)*
    ;


// equality/inequality (==/!=) (level 6)
equalityExpression
    :   relationalExpression ((NOT_EQUAL | EQUAL) relationalExpression)*
    ;


// boolean relational expressions (level 5)
relationalExpression
{
  String type = null;
}
    :   shiftExpression
        (   (   (   LT
                |   GT
                |   LE
                |   GE
                )
                shiftExpression
            )*
        |
            (INSTANCEOF type=typeSpec IDENT) =>
            INSTANCEOF type=typeSpec IDENT
        |
            INSTANCEOF type=typeSpec
        )
    ;


// bit shift expressions (level 4)
shiftExpression
    :   additiveExpression ((SL | SR | BSR) additiveExpression)*
    ;


// binary addition/subtraction (level 3)
additiveExpression
    :   multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;


// multiplication/division/modulo (level 2)
multiplicativeExpression
    :   unaryExpression ((STAR | DIV | MOD ) unaryExpression)*
    ;

unaryExpression
    :   INC unaryExpression
    |   DEC unaryExpression
    |   MINUS unaryExpression
    |   PLUS unaryExpression
    |   unaryExpressionNotPlusMinus[null, null]
    ;

/**
 * Unary expression which is not "+/- value".
 * <p/>
 * classCastStart/classCastEnd - a pair of marker tokens used to remember a class cast. This pair of markers is used to
 * solve type-inference for lambdaInc wrapper. For example:
 * <pre>
 *   interface Produce<T> { T produce(); }
 *   Object o = (Produce<String>)String::new;
 * </pre>
 * will be wrapped into lambdaInc like this:
 * <pre>
 *   Object o = (Produce<String>)lambdaInc(123, String::new);
 * </pre>
 * <p/>
 * The problem is that javac is unable to infer proper type of lambdaInc - it sees Object. In such case when Clover
 * finds a method reference with a class cast, it will memorize class cast start/end and copy it into a lambda wrapper:
 * <pre>
 *   Object o = (Produce<String>)lambdaInc(123, (Produce<String>)String::new);
 *         start^            end^               ^^^^^^copy^^^^^^^
 * </pre>
 *
 * The classCastStart and classCastEnd are being passed as arguments, because have a recursive call like this:
 * <pre>
 *    "(Produce<String>)String::new" is handled as
 *
 *    unaryExpressionNotPlusMinus[null, null] ->
 *    LPAREN classTypeSpec RPAREN unaryExpressionNotPlusMinus[start, end] ->
 *    postfixExpression[start, end]
 * </pre>
 */
unaryExpressionNotPlusMinus[CloverToken classCastStart, CloverToken classCastEnd]
{
    String type = null;
}
    :   BNOT unaryExpression
    |   LNOT unaryExpression

    |   (
            // subrule allows option to shut off warnings
            options {
                // "(int" ambig with postfixExpr due to lack of sequence
                // info in linear approximate LL(k).  It's ok.  Shut up.
                generateAmbigWarnings=false;
            }
        :
            // If typecast is built in type, must be numeric operand
            // Have to backtrack to see if operator follows
            (LPAREN type=builtInTypeSpec RPAREN unaryExpression)=>
            LPAREN type=builtInTypeSpec RPAREN
            unaryExpression
        |
            // Have to backtrack to see if operator follows.  If no operator
            // follows, it's a typecast.  No semantic checking needed to parse.
            // if it _looks_ like a cast, it _is_ a cast; else it's a "(expr)"
            (LPAREN type=classTypeSpec (BAND type=classOrInterfaceType)* RPAREN unaryExpressionNotPlusMinus[null, null])=>
            { classCastStart = lt(1); }
            LPAREN  type=classTypeSpec (BAND type=classOrInterfaceType)* RPAREN
            { classCastEnd = lt(0); }
            unaryExpressionNotPlusMinus[classCastStart, classCastEnd]
        |
            postfixExpression[classCastStart, classCastEnd]
        )
    ;

/**
 * Qualified names, array expressions, method invocation, post inc/dec
 *
 * @param classCastStart - used for instrumentation of a method reference with type cast
 * @param classCastEnd - used for instrumentation of a method reference with type cast
 */
postfixExpression[CloverToken classCastStart, CloverToken classCastEnd]
{
    /*
     * A marker token to remember where the method reference starts (like "Math::abs" or "String::new" or "int[]::new"
     * This is declared as a field and not as a local variable of a rule, because it must be shared between
     * postfixExpression (start and end of a reference) and supplementaryExpressionPart (end of an array constructor
     * reference).
     */
    CloverToken startMethodReference = null;
}
    :
        {
            // we might start a method reference, remember this token
            startMethodReference = lt(1);
        }
        primaryExpressionPart         // start with a primary part like constant or identifier
        supplementaryExpressionPart[classCastStart, classCastEnd, startMethodReference]   // add extra stuff like array indexes, this/super (optional)
        (
            (methodReferencePredicate) =>
            methodReferencePart           // add a method reference or
            {
                // we don't have a method signature here, so we'll use just a basic name
                Parameter[] la = new Parameter[0];
                MethodSignature methodReferenceSignature = new MethodSignature(
                        LambdaUtil.generateLambdaNameWithId(la, lambdaCounter++), null, null, la, null, new Modifiers());
                LambdaExpressionEntryEmitter expressionEntryEmitter = instrEnterLambdaMethodReference(
                        methodReferenceSignature, startMethodReference, classCastStart, classCastEnd);
                instrExitMethodReference(expressionEntryEmitter, lt(0));
            }
        |
            supplementaryPostIncDecPart   // possibly add on a post-increment or post-decrement.
        )
    ;



// the basic element of an expression
primaryExpressionPart returns [ContextSetAndComplexity ret]
{
    String type = null;
    ret = null;
}
    :
        IDENT
        {
            pushIdentifierToHeadStack(LT(0).getText());
        }
    |
        constant
    |
        TRUE
    |
        FALSE
    |
        THIS
        {
            pushIdentifierToHeadStack(LT(0).getText());
        }
    |
        NULL
    |
        newExpression
    |
        LPAREN! assignmentExpression RPAREN!
    |
        SUPER
        {
            pushIdentifierToHeadStack(LT(0).getText());
        }
    |
        // look for int.class, int[].class, and int[]::new
        type=builtInType
        ( LBRACK  RBRACK! )*
        (
            DOT CLASS
        |
            METHOD_REF NEW
        )
    |
        // hack: "non-sealed" in expression means "non - sealed", allow this to parse
        NON_SEALED
    |
        // a new lambda switch can be a part of an expression
        ( SWITCH LPAREN expression RPAREN LCURLY (CASE patternMatch | DEFAULT) LAMBDA) =>
        ret = lambdaSwitchExpression[null]
    |
        // even the old one colon switch has been retrofitted
        ( SWITCH LPAREN expression RPAREN LCURLY (CASE expression | DEFAULT) COLON) =>
        ret = colonSwitchExpression[null, true]
    ;

/**
 * A switch statement or expression containing one or more "case :" or "default :" conditions.
 * @param owningLabel a label before switch or null if not present
 * @param isInsideExpression true if the switch is part of an expression, false if is a standalone statement
 */
colonSwitchExpression [CloverToken owningLabel, boolean isInsideExpression] returns [ContextSetAndComplexity ret]
{
    CloverToken tmp = null;
    boolean labelled = (owningLabel != null);
    FlagDeclEmitter flag = null;
    ret = new ContextSetAndComplexity();
    int casesGroupComplexity;
}
    :
        sw:SWITCH
        {
            tmp = ct(sw);
            if (labelled) {
                tmp = owningLabel;
            }
            if (!isInsideExpression) {
                flag = declareFlagBefore(tmp);
            }
            enterContext(ContextStore.CONTEXT_SWITCH);
            ret.context = getCurrentContext();
        }
        LPAREN! expression RPAREN! LCURLY!
        (
            casesGroupComplexity = colonCasesGroup[flag]
            {
                ret.complexity += casesGroupComplexity;
            }
        )*
        {
            exitContext();
        }
        rc:RCURLY!
    ;

/**
 * A switch statement or expression containing one or more "case ->" or "default ->" conditions.
 */
lambdaSwitchExpression [CloverToken owningLabel] returns [ContextSetAndComplexity ret]
{
    int caseComplexity = 0;
    ContextSet saveContext = getCurrentContext();
    CloverToken tmp = null;
    ret = new ContextSetAndComplexity();
}
    :
        sw:SWITCH
        {
            tmp = ct(sw);
            enterContext(ContextStore.CONTEXT_SWITCH);
            ret.context = getCurrentContext();
        }
        LPAREN! expression RPAREN! LCURLY!
        (
            caseComplexity = lambdaCase[saveContext]
            {
                ret.complexity += caseComplexity;
            }
        )+
        {
            exitContext();
        }
        rc:RCURLY!
    ;

/**
 * A single "case x ->" or "default ->" label, followed by an expression or a block statement.
 */
lambdaCase[ContextSet context] returns [int complexity]
{
    CloverToken endTok = null;
    Token pos = null;
    CaseExpressionEntryEmitter expressionEntryEmitter = null;
    CaseThrowExpressionEntryEmitter throwEntryEmitter = null;
    complexity = 1;
}
    :
        (
            si1:CASE
            {
                constExpr = true;
            }
            patternMatch
            {
                constExpr = false;
                pos = si1;
            }
        |
            si2:DEFAULT
            {
                pos = si2;
            }
        )
        t:LAMBDA!
        (
            // throwing an exception must be instrumented differently
            (THROW expression SEMI) =>
            THROW expression SEMI
            {
                /* TODO calculate and pass expression's complexity */
                throwEntryEmitter = instrEnterCaseThrowExpression(ct(t), lt(0), context, 0);
                instrExitCaseThrowExpression(throwEntryEmitter, lt(0));
            }
        |
            // void and value-returning expressions
            expression SEMI
            {
                /* TODO calculate and pass expression's complexity */
                expressionEntryEmitter = instrEnterCaseExpression(ct(t), lt(0), context, 0);
                instrExitCaseExpression(expressionEntryEmitter, lt(0));
            }
        |
            // no need for special instrumentation, we will instrument it like a simple { } block, inside
            endTok=compoundStatement
        )
    ;

patternMatch
    :
        // just constants, string literals, true/false, null etc
        primaryExpressionPart
    ;

/**
 * A supplementary part for the primaryExpressionPart, which allows us to use array indexes, dot-qualified names,
 * this/class/super calls etc. Shall be used in conjunction with the primaryExpressionPart.
 *
 * @param classCastStart - used for instrumentation of a method reference with type cast
 * @param classCastEnd - used for instrumentation of a method reference with type cast
 */
supplementaryExpressionPart[CloverToken classCastStart, CloverToken classCastEnd, CloverToken startMethodReference]
    :
        (
            // qualified id (id.id.id.id...) -- build the name
            DOT (
                  (typeArguments)? // a prefix to a generic class to supply type arguments
                  IDENT
                      {
                          pushIdentifierToHeadStack(LT(0).getText());
                      }
                | THIS
                | CLASS
                | newExpression
                | SUPER // ClassName.super.field
                )
            // the above line needs a semantic check to make sure "class"
            // is the _last_ qualifier.
        |
            ( LBRACK  RBRACK! )+
            (
                // allow ClassName[].class
                DOT CLASS
            |
                // allow constructor reference for arrays, like "SomeType[]::new"
                METHOD_REF NEW
                {
                    // we don't have a method signature here, so well use just a basic name like
                    Parameter[] la = new Parameter[0];
                    MethodSignature methodReferenceSignature = new MethodSignature(
                            LambdaUtil.generateLambdaNameWithId(la, lambdaCounter++), null, null, la, null, new Modifiers());
                    LambdaExpressionEntryEmitter expressionEntryEmitter = instrEnterLambdaExpression(
                            methodReferenceSignature, startMethodReference, startMethodReference,
                            classCastStart, classCastEnd);
                    instrExitLambdaExpression(expressionEntryEmitter, lt(0));
                }
            )
        |
            // an array indexing operation
            LBRACK expression RBRACK!
        |
            // method invocation
            // The next line is not strictly proper; it allows x(3)(4) or
            //  x[2](4) which are not valid in Java.  If this grammar were used
            //  to validate a Java program a semantic check would be needed, or
            //   this rule would get really ugly...
            // It also allows ctor invocation like super(3) which is now
            // handled by the explicit constructor rule, but it would
            // be hard to syntactically prevent ctor calls here
            LPAREN argList RPAREN!
        )*
    ;

methodReferencePredicate
    :
        (typeArguments)? METHOD_REF
    ;

methodReferencePart
{
    String tp;
}
    :
        (typeArguments)?                // e.g. in "ArrayList <String>::<String>new"
        METHOD_REF                      // '::'
        (typeArguments)?
        ( IDENT | NEW )               // e.g. in "foo" / "<String>goo" / "<Integer>new"
    ;

/**
 * Possibly add on a post-increment or post-decrement.
 */
supplementaryPostIncDecPart
    :
        // note: allows INC/DEC on too much, but semantics can check
        (   INC
        |   DEC
        |   // nothing
        )
    ;

/** object instantiation.
 *  Trees are built as illustrated by the following input/tree pairs:
 * <pre>
 *  new T()
 *
 *  new
 *   |
 *   T --  ELIST
 *           |
 *          arg1 -- arg2 -- .. -- argn
 *
 *  new int[]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *
 *  new int[] {1,2}
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR -- ARRAY_INIT
 *                                  |
 *                                EXPR -- EXPR
 *                                  |      |
 *                                  1      2
 *
 *  new int[3]
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *                |
 *              EXPR
 *                |
 *                3
 *
 *  new int[1][2]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *               |
 *         ARRAY_DECLARATOR -- EXPR
 *               |              |
 *             EXPR             1
 *               |
 *               2
 * </pre>
 */
newExpression
{
    CloverToken endOfBlock = null;
    String typeParam = null;
}
    :   NEW (typeParam=typeParameters)? type
        (   LPAREN! argList RPAREN! (endOfBlock=classBlock[null])?

            //java 1.1
            // Note: This will allow bad constructs like
            //    new int[4][][3] {exp,exp}.
            //    There needs to be a semantic check here...
            // to make sure:
            //   a) [ expr ] and [ ] are not mixed
            //   b) [ expr ] and an init are not used together

        |   newArrayDeclarator (arrayInitializer)?
        )
    ;

argList
    :   (   expressionList
        |   /*nothing*/
        )
    ;

newArrayDeclarator
{
    AnnotationImpl ann = null;
}
    :   (
            // CONFLICT:
            // newExpression is a primaryExpressionPart which can be
            // followed by an array index reference.  This is ok,
            // as the generated code will stay in this loop as
            // long as it sees an LBRACK (proper behavior)
            options {
                warnWhenFollowAmbig = false;
            }
        :
            (ann=annotation)*
            LBRACK
                (expression)?
            RBRACK!
        )+
    ;

constant
    :   NUM_INT
    |   CHAR_LITERAL
    |   STRING_LITERAL
    |   NUM_FLOAT
    |   NUM_LONG
    |   NUM_DOUBLE
    ;


/////////////////////////////////////////////////
// annotations (JSR-175: Metadata facility)
/////////////////////////////////////////////////

// todo - collect annotations properly. we are just returning the name of the annotation, not any members
protected
annotation returns [AnnotationImpl anno]
    :
       anno = annotation2[false]
    ;

protected
annotation2 [boolean instrSuppressWarnings] returns [AnnotationImpl anno]
{
  boolean isSuppressWarnings = false;
  boolean ft = false;
  CloverToken ident = null; 
  boolean hasArgs = false;
  String annotationName = null;
  anno = new AnnotationImpl();
}
    :
        AT annotationName=identifier { ident=lt(0);isSuppressWarnings = instrSuppressWarnings && "SuppressWarnings".equals(annotationName);anno.setName(annotationName);}
        (
            lparen:LPAREN RPAREN // normalAnnotationRest
        |    (LPAREN IDENT ASSIGN )=> LPAREN annMemberValuePair[anno, isSuppressWarnings] (COMMA annMemberValuePair[anno, false])* RPAREN {hasArgs=true;} // normalAnnotation
        |    LPAREN ft=annMemberValue2[anno, "value", isSuppressWarnings, true] RPAREN {hasArgs=true;} // singleMemberAnnotation
           // none means just a markerAnnotation
        )?

        {
            if (isSuppressWarnings && !hasArgs) {
                suppressWarningsInstr = new SimpleEmitter("{\"fallthrough\"}");
                Emitter openParen = new SimpleEmitter("(");
                Emitter closeParen = new SimpleEmitter(")");
                CloverToken instrPoint = ct(lparen);
                if (instrPoint == null) {
                    instrPoint = ident;
                    suppressWarningsInstr.addDependent(openParen);
                    suppressWarningsInstr.addDependent(closeParen);
                    instrPoint.addPostEmitter(openParen);
                }
                instrPoint.addPostEmitter(suppressWarningsInstr);
                if (lparen == null) {
                    instrPoint.addPostEmitter(closeParen);
                }
            }
        }

    ;

protected
annMemberValuePair [AnnotationImpl anno, boolean isSuppressWarnings]
{
  boolean ft = false;
}
    :
        name:IDENT ASSIGN ft = annMemberValue2[anno, name.getText(), isSuppressWarnings, true]
    ;

protected
annMemberValue [boolean isSuppressWarnings, boolean makeArrayAndAdd] returns [boolean wasFallthroughLiteral]
	:
		  wasFallthroughLiteral = annMemberValue2[null, null, isSuppressWarnings, makeArrayAndAdd]
	;

protected
annMemberValue2 [AnnotationValueCollection anno, String key, boolean isSuppressWarnings, boolean makeArrayAndAdd] returns [boolean wasFallthroughLiteral]
{
  AnnotationImpl innerAnno = null;
  wasFallthroughLiteral = false;
  String expr = null;
}
    :
        (STRING_LITERAL) => t:STRING_LITERAL
        {   wasFallthroughLiteral = "\"fallthrough\"".equalsIgnoreCase(t.getText());
            if (isSuppressWarnings && makeArrayAndAdd) {

                if (wasFallthroughLiteral) {
                    // we don't need to set the suppression because one is already present
                    existingFallthroughSuppression = true;
                }
                else {
                    // put the string literal inside array initializer brackets, and add "fallthrough"
                    suppressWarningsInstr = new SimpleEmitter("{");
                    Emitter closeCurly = new SimpleEmitter("}");
                    Emitter comma = new SimpleEmitter(",");
                    Emitter fallthrough = new SimpleEmitter("\"fallthrough\"");
                    ct(t).addPreEmitter(suppressWarningsInstr);
                    ct(t).addPostEmitter(comma);
                    ct(t).addPostEmitter(fallthrough);
                    ct(t).addPostEmitter(closeCurly);
                    // adding as dependents allows them all to be turned off as a group
                    suppressWarningsInstr.addDependent(closeCurly);
                    suppressWarningsInstr.addDependent(comma);
                    suppressWarningsInstr.addDependent(fallthrough);
                }
            }
            if (anno != null) anno.put(key, new StringifiedAnnotationValue(t.getText()));
        }

        | expr = conditionalExpression2 {if (anno != null) anno.put(key, new StringifiedAnnotationValue(expr));}
        | innerAnno=annotation {if (anno != null) anno.put(key, innerAnno);}
        | annMemberValueArrayInitializer [anno, key, isSuppressWarnings]
    ;

protected
conditionalExpression2 returns [String asString]
{
	CloverToken start = lt(1);
	CloverToken end = null;
	asString = null;
}
	:
	conditionalExpression { end = lt(0); asString = TokenListUtil.getNormalisedSequence(start, end); }
	;

protected
annMemberValueArrayInitializer [AnnotationValueCollection anno, String key, boolean isSuppressWarnings]
{
  boolean emitComma = false;
  boolean seenFallthrough = false;
  CloverToken last = null;
  ArrayAnnotationValue annoArray = (anno == null) ? null : new ArrayAnnotationValue();
  if (anno != null) {
  	anno.put(key, annoArray);
  }
}
    :
        LCURLY
        (seenFallthrough = annMemberValues[annoArray, isSuppressWarnings] { emitComma = true;})?
        (COMMA{ emitComma=false; })?
        {
            CloverToken t = lt(0);
            if (isSuppressWarnings) {

                if (seenFallthrough) {
                    // we don't need to set the suppression because one is already present
                    existingFallthroughSuppression = true;
                }
                else {
                    // add "fallthrough" to existing array init
                    if (emitComma) {
                        suppressWarningsInstr = new SimpleEmitter(",");
                        ct(t).addPostEmitter(suppressWarningsInstr);
                    }
                    Emitter fallthrough = new SimpleEmitter("\"fallthrough\"");
                    ct(t).addPostEmitter(fallthrough);
                    if (suppressWarningsInstr == null) {
                        suppressWarningsInstr = fallthrough;
                    }
                    else {
                        suppressWarningsInstr.addDependent(fallthrough);
                    }
                }
            }
        }
        RCURLY
    ;

protected
annMemberValues [ArrayAnnotationValue annoArray, boolean isSuppressWarnings] returns [boolean sawFallthrough]
{
  sawFallthrough = false;
  boolean tmp;
}
    :
    	//null use as annotation array values are not named
        tmp=annMemberValue2[annoArray, null, isSuppressWarnings, false]
        {
            if (tmp) {
              sawFallthrough = true;
            }
        }
        (
	    	//null used as annotation array values are not named
            COMMA tmp=annMemberValue2[annoArray, null, isSuppressWarnings, false]
            {
                if (tmp) {
                    sawFallthrough = true;
                }
            }
            
        )*
    ;
