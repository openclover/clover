
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
/** Java 1.5/JSR14 Recognizer
 *
 * Based on the Public Domain Java 1.3 antlr grammar provided at
 * <a href="http://www.antlr.org">www.antlr.org</a>
 *
 *
 */
class JavaRecognizer extends Parser;
options {
    defaultErrorHandler = false;     // Don't generate parser error handlers
    k = 2;                           // two token lookahead
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
    IMPORT; UNARY_MINUS; UNARY_PLUS; CASE_GROUP; ELIST; FOR_INIT; FOR_CONDITION;
    FOR_ITERATOR; EMPTY_STAT; FINAL="final"; ABSTRACT="abstract";
    STRICTFP="strictfp"; SUPER_CTOR_CALL; CTOR_CALL;
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

    private ClassEntryNode enterClass(Modifiers mods, CloverToken tok, boolean aIsInterface, boolean aIsEnum, boolean isAnnotation) {
        return enterClass(null, mods, tok, aIsInterface, aIsEnum, isAnnotation, null);
    }

    private ClassEntryNode enterClass(Map<String, List<String>> tags, Modifiers mods, CloverToken tok, boolean aIsInterface, boolean aIsEnum, boolean isAnnotation, String superclass) {
        String classname = tok.getText();
        int startline = tok.getLine();
        int startcol = tok.getColumn();
        classnameList.add(classname);
        // the fullname of this class is the concatenation of
        // all containing classnames.
        String fullname = getClassname(classnameList);
        ClassEntryNode node = new ClassEntryNode(tags, mods, fullname, fileInfo.getPackageName(), superclass, getCurrentContext(), startline, startcol,
                topLevelClass, aIsInterface, aIsEnum, isAnnotation); 
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

    private String getClassname(List<String> classlist) {
        String fullname = "";
        String sep = "";
        for (String className : classlist) {
            fullname += sep + className;
            sep = ".";
        }
        return fullname;
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
                                                     CloverToken lcurly, CloverToken skip) {
        CloverToken instrPoint = (skip == null ? lcurly : skip);
        MethodRegistrationNode reg = new MethodRegistrationNode(getCurrentContext(), sig, start.getLine(), start.getColumn());
        lcurly.addPreEmitter(reg);

        MethodEntryInstrEmitter ret = new MethodEntryInstrEmitter(reg);
        instrPoint.addPostEmitter(ret);
        instrPoint.addPostEmitter(new DirectedFlushEmitter());
        return ret;
    }

    private MethodEntryInstrEmitter instrEnterMethod(MethodSignature sig, CloverToken start, CloverToken lcurly) {
        return instrEnterMethod(sig, start, lcurly, null);
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
     *
     * or in case when class cast is also present:
     *   before:   [(Integer)() -> 1 + 2]
     *   after :   [(Integer)RECORDER_INSTANCE_NAME.lambdaInc(777, (Integer)() -> 1 + 2]
     *
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
     *
     * or in case when class cast is also present:
     *   before:   [(Function<Integer,Integer>)Math::abs]
     *   after :   [(Function<Integer,Integer>)RECORDER_INSTANCE_NAME.lambdaInc(777, (Function<Integer,Integer>)Math::abs]
     *
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
                    methodReferenceStart.getLine(), methodReferenceStart.getColumn(), methodReferenceStart.getLine(), methodReferenceStart .getColumn());
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
     *
     * or in case when class cast is also present:
     *   before:   [(Integer)() -> 1 + 2]
     *   after :   [(Integer)() -> {RECORDER_INSTANCE_NAME.inc(777); return 1 + 2]
     *
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
     private LambdaExprToBlockStartEntryEmitter instrEnterLambdaExprToBlockExpression(MethodSignature lambdaSignature, CloverToken lambdaStart, CloverToken bodyStart) {
        if (cfg.getInstrumentLambda() == LambdaInstrumentation.ALL
                || cfg.getInstrumentLambda() == LambdaInstrumentation.ALL_BUT_REFERENCE
                || cfg.getInstrumentLambda() == LambdaInstrumentation.EXPRESSION) {
             LambdaExprToBlockStartEntryEmitter startEmitter = new LambdaExprToBlockStartEntryEmitter(lambdaSignature, lambdaStart.getLine(), lambdaStart.getColumn());
             LambdaExprToBlockBodyEntryEmitter bodyEmitter = new LambdaExprToBlockBodyEntryEmitter(startEmitter, bodyStart.getLine(), bodyStart.getColumn());
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
             tok.addPostEmitter(new LambdaExprToBlockExitEmitter(entryEmitter, tok.getLine(), tok.getColumn()+tok.getText().length()));
         }
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
              end.getColumn() + end.getText().length(), 1/*TODO*/));
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
              end.addPreEmitter(new EndBoolInstrEmitter(getCurrentContext(), begin.getLine(), begin.getColumn() - 1,
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

}

//
// module-info.java grammar
//
// Note: none of the keywords used in module-info (module, exports, requires etc) is not a "real keyword".
// You can still use them as identifiers. Therefore, we cannot use string literals explicitly, e.g.
//     "xyz" identifier SEMI
// because string literal will become a token and "steal" this keyword from IDENT, failing on parsing regular code.
// Instead of this we use a trick to to sneak peek the next token and treat it as a simple IDENT
//      { LT(1).getText().equals("xyz") }? IDENT identifier SEMI
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
            { LT(1).getText().equals("open") && LT(2).getText().equals("module") }? IDENT IDENT
        |
            { LT(1).getText().equals("module") }? IDENT
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
            { LT(1).getText().equals("open") }? IDENT
        )?
        { LT(1).getText().equals("module") }? IDENT
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
        { LT(1).getText().equals("requires") }? IDENT
        (
            { LT(1).getText().equals("transitive") }? IDENT
        |
            "static"   // static is java keyword so we don't use a trick like above
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
        { LT(1).getText().equals("exports") }? IDENT
        exportedPackage=identifier
        (
            { LT(1).getText().equals("to") }? IDENT
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
        { LT(1).getText().equals("opens") }? IDENT
        openedPackage=identifier
        (
            { LT(1).getText().equals("to") }? IDENT
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
        { LT(1).getText().equals("uses") }? IDENT
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
        { LT(1).getText().equals("provides") }? IDENT
        serviceName=identifier
        { LT(1).getText().equals("with") }? IDENT
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
    :   // A compilation unit starts with an optional package definition
        (   (packageDefinition)=> packageDefinition
            // need above syntactic predicate to dis-amb the 'annotation' leading both
            // packageDefinition and typeDefinition
        |   /* nothing */
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
    :   (ann=annotation)*
        "package"  pn=identifier SEMI! {fileInfo.setPackageName(pn);}
    ;


// Import statement: import followed by a package or class name
//    or a "static" method import
importDefinition
    options {defaultErrorHandler = false;}
    :   "import" ("static")? identifierStar SEMI!
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
            first = (CloverToken)LT(1);
        }
        mods=classOrInterfaceModifiers[!nested]!
        typeDefinition2[mods, first, nested]
    |   SEMI!
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
        |   name=interfaceDefinition[mods]
        |   name=enumDefinition[mods] {isEnum=true;}
        |   name=annotationTypeDeclaration[mods]
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
    : spec = classTypeSpec
    | spec = builtInTypeSpec
    ;

arraySpecOpt returns [String brackets]
{
 brackets = "";
}

    :

        (options{greedy=true;}: // match as many as possible
            LBRACK RBRACK
            {brackets += "[]";}
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
    :   typeSpec = classOrInterfaceType
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
}


:       {first = (CloverToken)LT(1);}
        IDENT (typeArguments)?
        (options{greedy=true;}: // match as many as possible
            DOT
            IDENT (typeArguments)?
        )*
        {
            last = (CloverToken)LT(0);
            type = TokenListUtil.getNormalisedSequence(first, last);
        }
    ;

typeArguments
{int currentLtLevel = 0;}
    :
        {currentLtLevel = ltCounter;}
        LT {ltCounter++;}
        (
            options{generateAmbigWarnings=false;}:

            singleTypeArgument
            (options{greedy=true;}: // match as many as possible
                COMMA singleTypeArgument
            )*
        )?

        (   // turn warning off since Antlr generates the right code,
            // plus we have our semantic predicate below
            options{generateAmbigWarnings=false;}:
            typeArgumentsEnd
        )?

        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?
    ;

singleTypeArgument {
  String type = null;
}
    :
        (
            type=classTypeSpec | type=builtInTypeSpec | QUESTION
        )

        (   // I'm pretty sure Antlr generates the right thing here:
            options{generateAmbigWarnings=false;}:
            ("extends"|"super") (type=classTypeSpec | type=builtInTypeSpec | QUESTION)
        )?
    ;

// this gobbles up *some* amount of '>' characters, and counts how many
// it gobbled.
protected typeArgumentsEnd:
        GT {ltCounter-=1;}
    |   SR {ltCounter-=2;}
    |   BSR {ltCounter-=3;}
    ;

// A builtin type specification is a builtin type with possible brackets
// afterwards (which would make it an array type).
builtInTypeSpec returns [String spec]
{
    String arrayOpt = "";
}
    :   spec = builtInType
        arrayOpt = arraySpecOpt
        {
            spec += arrayOpt;
        }
    ;

// A type name. which is either a (possibly qualified and parameterized)
// class name or a primitive (builtin) type
type {
  String spec = null;
}
    :   spec=classOrInterfaceType
    |   spec=builtInType
    ;

// The primitive types.
builtInType returns [String type]
{
    type = "";
}
    : (  "void"
    |   "boolean"
    |   "byte"
    |   "char"
    |   "short"
    |   "int"
    |   "float"
    |   "long"
    |   "double" ) { type = LT(0).getText();}
    ;

// A (possibly-qualified) java identifier.  We start with the first IDENT
//   and expand its name by adding dots and following IDENTS
// returns the identifier as a string (with all the dots)
identifier returns [String str]
{
    StringBuffer buf = new StringBuffer();
    str = null;
}
    :   i1:IDENT  {buf.append(i1.getText());}
        (
            DOT      {buf.append('.');}
            i2:IDENT {buf.append(i2.getText());}
        )*

        {
            str = buf.toString();
        }
    ;

identifierStar
    :   IDENT
        ( DOT IDENT )*
        ( DOT STAR  )?
    ;

/**
 * A list of zero or more modifiers (including annotations) in the class, enum or interface signature.
 */
classOrInterfaceModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    int m = 0;
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
classOrInterfaceModifier returns [int m]
{
    int im;
    m = 0;
}
    :   "private"       { m=java.lang.reflect.Modifier.PRIVATE; }
    |   "protected"     { m=java.lang.reflect.Modifier.PROTECTED; }
    |   "public"        { m=java.lang.reflect.Modifier.PUBLIC; }
    |   "abstract"      { m=java.lang.reflect.Modifier.ABSTRACT; }
    |   "final"         { m=java.lang.reflect.Modifier.FINAL; }    // for classes only
    |   "static"        { m=java.lang.reflect.Modifier.STATIC; }
    |   "strictfp"      { m=java.lang.reflect.Modifier.STRICT; }
    ;

/**
 * A list of zero or more modifiers in the field signature, including annotations.
 */
fieldModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    int m = 0;
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
    :   "private"       { m=java.lang.reflect.Modifier.PRIVATE; }
    |   "protected"     { m=java.lang.reflect.Modifier.PROTECTED; }
    |   "public"        { m=java.lang.reflect.Modifier.PUBLIC; }
    |   "final"         { m=java.lang.reflect.Modifier.FINAL; }
    |   "static"        { m=java.lang.reflect.Modifier.STATIC; }
    |   "transient"     { m=java.lang.reflect.Modifier.TRANSIENT; }
    |   "volatile"      { m=java.lang.reflect.Modifier.VOLATILE; }
    ;

/**
 * A list of zero or more modifiers in the method signature, including annotations.
 */
methodModifiers [boolean instrSuppressWarnings] returns [Modifiers mods]
{
    mods = new Modifiers();
    int m = 0;
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
methodModifier returns [int m]
{
    m = 0;
}
    :   "private"       { m=java.lang.reflect.Modifier.PRIVATE; }
    |   "protected"     { m=java.lang.reflect.Modifier.PROTECTED; }
    |   "public"        { m=java.lang.reflect.Modifier.PUBLIC; }
    |   "abstract"      { m=java.lang.reflect.Modifier.ABSTRACT; }
    |   "final"         { m=java.lang.reflect.Modifier.FINAL; }
    |   "native"        { m=java.lang.reflect.Modifier.NATIVE; }
    |   "static"        { m=java.lang.reflect.Modifier.STATIC; }
    |   "strictfp"      { m=java.lang.reflect.Modifier.STRICT; }
    |   "synchronized"  { m=java.lang.reflect.Modifier.SYNCHRONIZED; }

    // not a true modifier, used only to mark virtual extension method in an interface, we keep information about it
    |   "default"       { m=com.atlassian.clover.registry.entities.Modifier.DEFAULT; }
    ;

// Definition of a Java class
classDefinition! [Modifiers mods] returns [String classname]
{
	CloverToken first = (CloverToken)LT(0);
	Map<String, List<String>> tags = null;
    boolean deprecated = false;
    CloverToken endOfBlock = null;
    String superclass = null;
    ClassEntryNode classEntry = null;
    classname = null;
    String typeParam = null;
}
    :   "class" {tags = TokenListUtil.getJDocTagsAndValuesOnBlock(first); deprecated = maybeEnterDeprecated(first);}
        id:IDENT
        // it _might_ have type paramaters
        (typeParam=typeParameters)?

        // it _might_ have a superclass...
        superclass = superClassClause
        // it might implement some interfaces...
        implementsClause
        {
            classEntry = enterClass(tags, mods, (CloverToken)id, false, false, false, superclass);
        }
        // now parse the body of the class
        endOfBlock = classBlock[classEntry]
        {
            exitClass(endOfBlock, classEntry); maybeExitDeprecated(deprecated);
            classname = id.getText();
        }
    ;

superClassClause! returns [String superclass]
{
   superclass = null;
}
    :   ( "extends" superclass=classOrInterfaceType )?
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
    :   "interface" {deprecated = maybeEnterDeprecated((CloverToken)LT(0));}
        id:IDENT
        // it _might_ have type paramaters
        (typeParam=typeParameters)?
        {
            classEntry = enterClass(mods, (CloverToken)id, true, false, false);
        }
        // it might extend some other interfaces
        interfaceExtends
        // now parse the body of the interface (looks like a class...)
        endOfBlock = classBlock[classEntry]
        {
            exitClass(endOfBlock, classEntry); maybeExitDeprecated(deprecated);
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
    :    "enum"
        {deprecated = maybeEnterDeprecated((CloverToken)LT(0));}

        id:IDENT

        {
            classEntry = enterClass(mods, (CloverToken)id, false, true, false);
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
        AT "interface" id:IDENT
        {
            classEntry = enterClass(mods, (CloverToken)id, false, false, true);
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
  CloverToken start = (CloverToken)LT(1);
  asString = null;
}
    :
        {currentLtLevel = ltCounter;}
        LT {ltCounter++;}
        typeParameter (COMMA typeParameter)*
        (typeArgumentsEnd)?
        // make sure we have gobbled up enough '>' characters
        // if we are at the "top level" of nested typeArgument productions
        {(currentLtLevel != 0) || ltCounter == currentLtLevel}?

        {asString = TokenListUtil.getNormalisedSequence(start, (CloverToken)LT(0));}
    ;

typeParameter
{
   String type = null;
}
    :
        (IDENT|QUESTION)
        (   // I'm pretty sure Antlr generates the right thing here:
            options{generateAmbigWarnings=false;}:
            "extends" type=classOrInterfaceType
            (BAND type=classOrInterfaceType)*
        )?
    ;

// This is the body of a class.  You can have fields and extra semicolons,
// That's about it (until you see what a field is...)
classBlock [ClassEntryNode classEntry] returns [CloverToken t]
{t = null;}
    :   ip:LCURLY! { setRecorderMemberInsertPoint(classEntry, (CloverToken)ip);}
            ( field[classEntry] | SEMI! )*
        rc:RCURLY! {t = (CloverToken)rc;}
    ;

enumBlock [ClassEntryNode classEntry] returns [CloverToken t]
{
    boolean topLevelSave = topLevelClass;
    t = null;
}
    :   LCURLY!
        (
            options {warnWhenFollowAmbig = false;}:
            (
                enumConstant
                (
                    // CONFLICT: does a COMMA after an enumConstant start a new
                    //           constant or start the optional ',' at end?
                    //           ANTLR generates proper code by matching
                    //           the comma as soon as possible.
                    options {warnWhenFollowAmbig = false;}:
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
                (  field[null] | SEMI! )*
                {topLevelClass = topLevelSave;}

            )?
        )?
        ip:RCURLY!
        { t = (CloverToken)ip; setRecorderMemberInsertPoint(classEntry, t); }
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
        { setRecorderMemberInsertPoint(classEntry, (CloverToken)ip); }
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
                ( classOrInterfaceModifiers[false] ( "class" | "interface" | AT "interface" | "enum" ) ) =>

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
            t = (CloverToken)endOfBlock;
        }
    ;

protected
annDefaultValue
{
  boolean ft = false;
}
    :
        "default" ft=annMemberValue[false,false]
    ;

// An interface can extend several other interfaces...
interfaceExtends
{
  String type = null;
}    :   (
        "extends"!
        type=classOrInterfaceType ( COMMA! type=classOrInterfaceType )*
        )?
    ;

// A class can implement several interfaces...
implementsClause
{
   String type = null;
}
    :   (
            "implements"! type=classOrInterfaceType ( COMMA! type=classOrInterfaceType )*
        )?
    ;

// Now the various things that can be defined inside a class or interface...
// Note that not all of these are really valid in an interface (constructors,
//   for example), and if this grammar were used for a compiler there would
//   need to be some semantic checks to make sure we're doing the right thing...
field! [ClassEntryNode containingClass]
{
    CloverToken tmp;
    CloverToken first = (CloverToken)LT(1);
    CloverToken endSig = null;
    String typeParam = null;
    boolean deprecated = false;
    Modifiers mods = null;
    String name = null;
    MethodSignature signature = null;
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
            // INNER CLASSES, INTERFACES, ENUMS, ANNOTATIONS
            // look further to recognize that it's a definition of an inner type
            ( classOrInterfaceModifiers[false] ( "class" | "interface" | AT "interface" | "enum" ) ) =>

            mods=classOrInterfaceModifiers[false]
            { deprecated = maybeEnterDeprecated(tags, mods); }
            (
                typename = classDefinition[mods]       // inner class
            |   typename = interfaceDefinition[mods]   // inner interface
            |   typename = enumDefinition[mods]   // inner enum
            |   typename = annotationTypeDeclaration[mods] // inner annotation decl
            )

        |
            // CONSTRUCTORS OR METHODS
            // look further to recognize that it's a constructor or method, e.g.:
            //   "public MyClass ..."
            //   "private <T> List<T> foo("
            ( mods=methodModifiers[false] (typeParameters)? (ctorHead[tags, mods, first] | typeSpec IDENT LPAREN) ) =>

            // common part
            mods=methodModifiers[false]
            { deprecated = maybeEnterDeprecated(tags, mods); }
            (typeParam=typeParameters)?

            (
                // constructor-specific part

                // TODO - collect more of the ctor signature here
                signature = ctorHead[tags, mods, first]
                {
                    endSig = (CloverToken)LT(0);
                }
                s:constructorBody[signature, first, endSig] // constructor body
            |
                // method-specific part

                returnType=typeSpec              // return type of the method
                methodName:IDENT                 // the name of the method

                // parse the formal parameter declarations.
                LPAREN! parameters=parameterDeclarationList RPAREN!

                brackets = declaratorBrackets

                // get the list of exceptions that this method is
                // declared to throw
                (throwsTypes=throwsClause)?
                {
                    endSig = (CloverToken)LT(0);
                    // todo - add parameters to method signature
                    signature = new MethodSignature(first, (CloverToken)methodName, endSig, tags, mods, methodName.getText(),
                                                    typeParam, returnType+brackets, parameters, throwsTypes);
                }
                (outerCompoundStmt[signature, first, endSig, ContextStore.CONTEXT_METHOD] | SEMI )
            )

        |
            // FIELDS
            // look further to recognize that it's a field
            ( fieldModifiers[false] typeSpec variableDefinitions ) =>

            mods=fieldModifiers[false]
            { deprecated = maybeEnterDeprecated(tags, mods); }
            returnType=typeSpec
            variableDefinitions
            SEMI
        )
        { maybeExitDeprecated(deprecated); }

    // "static { ... }" class initializer
    |   {
            deprecated = maybeEnterDeprecated(first);
            signature = new MethodSignature(null, null, null, "<clinit>, line " + first.getLine(), null, null, null, null);
        }

        "static" outerCompoundStmt[signature, first, null, ContextStore.CONTEXT_STATIC]

        { maybeExitDeprecated(deprecated); }

    // ...or "{ ... }" instance initializer
    |
        {
            deprecated = maybeEnterDeprecated(first);
            signature = new MethodSignature(null, null, null, "<init>, line " + first.getLine(), null, null, null, null);
        }

        outerCompoundStmt[signature, first, null, ContextStore.CONTEXT_INSTANCE]

        { maybeExitDeprecated(deprecated); }

    ;

constructorBody[MethodSignature signature, CloverToken start, CloverToken endSig]
{
  CloverToken endOfInv = null;
  CloverToken tmp;
}
    :   {enterContext(ContextStore.CONTEXT_CTOR);}

        lc:LCURLY
        // Predicate might be slow but only checked once per constructor def
        // not for general methods.
        (   (explicitConstructorInvocation) => endOfInv = explicitConstructorInvocation
        | )
        (tmp=statement[null])*
        rc:RCURLY!

        {
         // special case for instrumenting entry to ctors - HACK add ctor sig for completeness
         MethodEntryInstrEmitter entry = instrEnterMethod(signature, start, (CloverToken)lc, endOfInv);
         instrExitMethod(entry, (CloverToken)rc);
         exitContext();
         fileInfo.addMethodMarker(entry, start, endSig, (CloverToken)rc);
        }
    ;

explicitConstructorInvocation returns [CloverToken t]
{
    t = null;
}
    :   (   options {
                // this/super can begin a primaryExpressionPart too; with finite
                // lookahead ANTLR will think the 3rd alternative conflicts
                // with 1, 2.  I am shutting off warning since ANTLR resolves
                // the nondeterminism by correctly matching alts 1 or 2 when
                // it sees this( or super(
                generateAmbigWarnings=false;
            }

        :   pos1:"this"! LPAREN argList RPAREN! t1:SEMI!
            {
                t=instrInlineAfter((CloverToken)t1, (CloverToken)pos1, (CloverToken)t1);
            }

        |   pos2:"super"! lp2:LPAREN^ argList RPAREN! t2:SEMI!
            {
                t=instrInlineAfter((CloverToken)t2, (CloverToken)pos2, (CloverToken)t2);
            }

            // (new Outer()).super()  (create enclosing instance)
        |   primaryExpressionPart
            (DOT! "this")? // HACK see CCD-264 - explicit ctor invocation can have form ClassName.this.super(..)
            DOT! pos3:"super"! lp3:LPAREN^ argList RPAREN! t3:SEMI!
            {
                t=instrInlineAfter((CloverToken)t3, (CloverToken)pos3, (CloverToken)t3);
            }
        )
    ;

variableDefinitions
    :   variableDeclarator (COMMA! variableDeclarator)*
    ;

/** Declaration of a variable.  This can be a class/instance variable,
 *   or a local variable in a method
 * It can also include possible initialization.
 */
variableDeclarator!
{
  String brackets = null;
}
    :   IDENT brackets=declaratorBrackets varInitializer
    ;

declaratorBrackets returns [String brackets]
{
    brackets = "";
}
    :   (LBRACK RBRACK! {brackets += "[]";})*
    ;

varInitializer
    :   ( ASSIGN initializer )?
    ;

// This is an initializer used to set up an array.
arrayInitializer
    :   LCURLY
            (   initializer
                (
                    // CONFLICT: does a COMMA after an initializer start a new
                    //           initializer or start the option ',' at end?
                    //           ANTLR generates proper code by matching
                    //           the comma as soon as possible.
                    options {
                        warnWhenFollowAmbig = false;
                    }
                :
                    COMMA! initializer
                )*

            )?
            (COMMA!)?
        RCURLY!
    ;


// The two "things" that can initialize an array element are an expression
//   and another (nested) array initializer.
initializer
    :   expression
    |   arrayInitializer
    ;

// This is the header of a method.  It includes the name and parameters
//   for the method.
//   This also watches for a list of exception classes in a "throws" clause.
ctorHead [Map tags, Modifiers mods, CloverToken first] returns [MethodSignature sig]
{
 sig = null;
 Parameter [] params = null;
 String [] throwsTypes = null;
}
    :   (ctorName:IDENT  // the name of the method
        // parse the formal parameter declarations.
        LPAREN! params=parameterDeclarationList RPAREN!

        // get the list of exceptions that this method is declared to throw
        (throwsTypes=throwsClause)?)
        {
            CloverToken endSig = (CloverToken)LT(0);
            sig = new MethodSignature(first, (CloverToken)ctorName, endSig, tags, mods, ctorName.getText(), null, null, params, throwsTypes);
         }
    ;

// This is a list of exception classes that the method is declared to throw
throwsClause returns [String [] throwsTypes]
{
    List<String> throwsList = new ArrayList<String>();
    throwsTypes = null;
    String id;
}
    :   "throws" id=identifier {throwsList.add(id);} ( COMMA! id=identifier {throwsList.add(id);})*
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

    :   ( param=parameterDeclaration {parameters.add(param);} ( COMMA! param=parameterDeclaration {parameters.add(param);})* )?
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
    :   parameterModifier type=typeSpec (ELLIPSIS)? i:IDENT
        brackets=declaratorBrackets
        {
          parameter = new Parameter(type+brackets,i.getText());
        }
    ;

parameterModifier
{
  AnnotationImpl ann = null;
}
    :   (ann=annotation)*
        (f:"final" (ann=annotation)* )?
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
        param=implicitParameterDeclaration { parameterList.add(param); }
        ( COMMA! param=implicitParameterDeclaration { parameterList.add(param); } )*
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
//   As a completely indepdent braced block of code inside a method
//      it starts a new scope for variable definitions
compoundStatement returns [CloverToken t]
{t = null;}
    :   LCURLY
            // include the (possibly-empty) list of statements
            (t=statement[null])*
        rc:RCURLY! {t = (CloverToken)rc;}
    ;

outerCompoundStmt [MethodSignature sig, CloverToken start, CloverToken endSig, int context]
{
  CloverToken tmp;
}
    :   {enterContext(context);}
        lc:LCURLY
            // include the (possibly-empty) list of statements
            (tmp=statement[null])*
        rc:RCURLY!
        {
            MethodEntryInstrEmitter entry = instrEnterMethod(sig, start, (CloverToken)lc);
            instrExitMethod(entry, (CloverToken)rc);
            exitContext();
            if (context == ContextStore.CONTEXT_METHOD) fileInfo.addMethodMarker(entry, start, endSig, (CloverToken)rc);
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
}

    : {first = (CloverToken)LT(1);}
    (
        // an assert statement
        "assert"
        { enterContext(ContextStore.CONTEXT_ASSERT); instrumentable = false; saveContext = getCurrentContext(); }
        { tmp=(CloverToken)LT(1); }
        expression
        ( colon:COLON! {instrBoolExpr(tmp, (CloverToken)colon); assertColonPart=true;}  expression )?
        semi:SEMI!
        {
            if (!assertColonPart) {
                 instrBoolExpr(tmp, (CloverToken)semi);
            }
            exitContext();
        }

    // A list of statements in curly braces -- start a new scope!

    |  tmp = compoundStatement {matchable = false; instrumentable = false;}

    // declarations are ambiguous with "ID DOT" relative to expression
    // statements.  Must backtrack to be sure.  Could use a semantic
    // predicate to test symbol table to see what the type was coming
    // up, but that's pretty hard without a symbol table ;)
    |  (declaration)=> declaration se1:SEMI! {flushAfter = (CloverToken)se1;}

    // An expression statement.  This could be a method call,
    // assignment statement, or any other expression evaluated for
    // side-effects.
    |   expression se2:SEMI! { flushAfter = (CloverToken)se2; }

    // class definition
    |   mods=classOrInterfaceModifiers[false]! classname=classDefinition[mods] { instrumentable = false; }//##TODO - return last token

    // Attach a label to the front of a statement
    |   IDENT COLON {labelTok = owningLabel; if (!labelled) labelTok = first; } last = statement[labelTok]

    // If-else statement
    |   "if" { enterContext(ContextStore.CONTEXT_IF); saveContext = getCurrentContext();}
        LPAREN! { tmp=(CloverToken)LT(1); } expression rp1:RPAREN!
        { instrBoolExpr(tmp, (CloverToken)rp1); addOpenBraceAfter((CloverToken)rp1); }
          last = statement[null] {addCloseBraceAfter(last); exitContext();}
        (
            // CONFLICT: the old "dangling-else" problem...
            //           ANTLR generates proper code matching
            //           as soon as possible.  Hush warning.
            options {
                warnWhenFollowAmbig = false;
            }
        :
            el:"else"! {addOpenBraceAfter((CloverToken)el);enterContext(ContextStore.CONTEXT_ELSE); saveContext = getCurrentContext();}
            last = statement[null] {addCloseBraceAfter(last);exitContext();}
        )?


    // For statement
    |   "for"
        { enterContext(ContextStore.CONTEXT_FOR);  saveContext = getCurrentContext();}
        LPAREN!
        (
            (parameterDeclaration COLON) =>
            ( // enhanced for
                parameter=parameterDeclaration COLON expression
            )
        |
            ( // traditional for
                forInit SEMI!   // initializer
                forCond    // condition test
                forIter         // updater
            )
        )
        rp:RPAREN!{addOpenBraceAfter((CloverToken)rp);}
        last = statement[null]   // statement to loop over
        {addCloseBraceAfter(last); exitContext();}

    // While statement
    |   "while"
        { enterContext(ContextStore.CONTEXT_WHILE);  saveContext = getCurrentContext();}
        LPAREN! { tmp = (CloverToken)LT(1); } expression rp2:RPAREN!
        {instrBoolExpr(tmp, (CloverToken)rp2); addOpenBraceAfter((CloverToken)rp2);}
           last = statement[null]
        {addCloseBraceAfter(last); exitContext();}

    // do-while statement
    |   d1:"do"
        { addOpenBraceAfter((CloverToken)d1);
            enterContext(ContextStore.CONTEXT_DO);  saveContext = getCurrentContext();}
           tmp=statement[null]
        {addCloseBraceAfter(tmp);exitContext();}
        "while"! LPAREN! { tmp=(CloverToken)LT(1); } expression rp3:RPAREN!
            {instrBoolExpr(tmp, (CloverToken)rp3);} sem:SEMI! {flushAfter = (CloverToken)sem;}

    // get out of a loop (or switch)
    |   "break" (IDENT)? SEMI!

    // do next iteration of a loop
    |   "continue" (IDENT)? SEMI!

    // Return an expression
    |   "return" (expression)? SEMI!

    // switch/case statement
    |   sw:"switch"
        { tmp = (CloverToken)sw;
          if (labelled) {
             tmp = owningLabel;
          }
          flag = declareFlagBefore(tmp);
          enterContext(ContextStore.CONTEXT_SWITCH);
          saveContext = getCurrentContext();

        }
        LPAREN! expression RPAREN! LCURLY!
            ( tmpCmp = casesGroup[flag] { complexity += tmpCmp; })*
        {exitContext();}
        rc:RCURLY!

    // exception try-catch block
    |   (tryCatchBlock[labelled]) => last = tryCatchBlock[labelled] {instrumentable = false; /* instrumentation happens in the tryCatchBlock rule */}

    // throw an exception
    |   "throw" expression SEMI!

    // synchronize a statement
    |   "synchronized" LPAREN! expression RPAREN!
        {
            enterContext(ContextStore.CONTEXT_SYNC);
            saveContext = getCurrentContext();
        }
        last = compoundStatement
        { exitContext();}

    // empty statement
    |   SEMI
    )
        {
            if (last == null) {
                last = (CloverToken)LT(0);
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

casesGroup[FlagDeclEmitter flag] returns [int complexity]
{
    int tmp = 0;
    complexity = 0;
}
    :   (   // CONFLICT: to which case group do the statements bind?
            //           ANTLR generates proper code: it groups the
            //           many "case"/"default" labels together then
            //           follows them with the statements
            options {
                warnWhenFollowAmbig = false;
            }
            :
            tmp = aCase[flag] {complexity += tmp;}
        )+
        caseSList
    ;

aCase[FlagDeclEmitter flag] returns [int complexity]
{
    Token pos = null;
    complexity = 0;
}
    :   ( si1:"case" {constExpr = true;} expression {constExpr = false; pos = si1; complexity++;}
        | si2:"default"         {pos = si2;}
        )
        t:COLON!
        {
            instrInlineAfter((CloverToken)t, (CloverToken)pos, (CloverToken)t, flag);
            fileInfo.setSuppressFallthroughWarnings(true);
        }
    ;

caseSList {
  CloverToken tmp;
}
    :   (tmp=statement[null])*
    ;

// The initializer for a for loop
forInit
        // if it looks like a declaration, it is
    :   (   (declaration)=> declaration
        // otherwise it could be an expression list...
        |   expressionList
        )?
    ;

forCond {
  CloverToken tmp = null;
}
    :   ({ tmp=(CloverToken)LT(1);  } expression se:SEMI!{instrBoolExpr(tmp, (CloverToken)se);}) | SEMI!
    ;

forIter
    :   (expressionList)?
    ;

// an exception handler try/catch block
tryCatchBlock [boolean labelled] returns [CloverToken last]
{
  last = null;
  int complexity = 0;
  ContextSet saveContext = getCurrentContext();
}
    :   tr:"try" (lp:LPAREN {insertAutoCloseableClassDecl((CloverToken)tr);} declaration {complexity++; instrArmDecl(((CloverToken)lp).getNext(), (CloverToken)LT(0), saveContext);} (semi:SEMI declaration {complexity++; instrArmDecl(((CloverToken)semi).getNext(), (CloverToken)LT(0), saveContext);})* (SEMI)? rp:RPAREN )?
        {enterContext(ContextStore.CONTEXT_TRY); saveContext = getCurrentContext();}
            last=compoundStatement
        {exitContext();}
        (last=handler {complexity++;})*
        ( "finally"
            {enterContext(ContextStore.CONTEXT_FINALLY); saveContext = getCurrentContext();}
            last=compoundStatement
            {exitContext();}
        )?
        { if (!labelled) {
                instrInlineBefore((CloverToken)tr, last, saveContext, complexity);
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
    :   "catch"
        LPAREN!
        ( an=annotation2[false] )*
        ("final")?
        ( an=annotation2[false] )*
        ts=typeSpec
        (BOR ts=typeSpec)*
        IDENT
        RPAREN!
        {enterContext(ContextStore.CONTEXT_CATCH);}
        last=compoundStatement
        {exitContext();}
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
 *
 * where body is in a form of a single statement or expression, e.g.:
 *   <code>System.out.println("Hello")</code>
 *   <code>x + y</code>
 *
 * or in a form of the code block, e.g.:
 *   <pre>
 *   {
 *      the;
 *      code;
 *      block;
 *   }
 *   </pre>
 *
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

    /**
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
                { classCastStart = (CloverToken)LT(1); }
                LPAREN type=classTypeSpec
                (BAND typeExt=classOrInterfaceType { type += "&" + typeExt; } )*
                RPAREN
                { classCastEnd = (CloverToken)LT(0); }
                { startLambdaArgs = (CloverToken)LT(1); }
                la=lambdaArgs
                LAMBDA
        |
            (la=lambdaArgs LAMBDA) =>
                { startLambdaArgs = (CloverToken)LT(1); }
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
                startLambdaBody = (CloverToken)LT(1);
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
                    instrExitLambdaExprToBlockExpression(exprToBlockStartEntryEmitter, (CloverToken)LT(0));
                } else {
                    instrExitLambdaExpression(expressionEntryEmitter, (CloverToken)LT(0));
                }
            }
        |
            {
                // expected LT(1)=LCURLY
                blockEntryEmitter = instrEnterLambdaBlock(lambdaSignature, (CloverToken)LT(1));
            }
            cs=compoundStatement
            {
                // expected LT(0)=RCURLY
                instrExitLambdaBlock(blockEntryEmitter, (CloverToken)LT(0));
            }
        )
    ;

/**
 * List of formal arguments for lambda function definition. Possible forms are:
 * 1) Empty list of arguments for a lambda function, i.e.:
 *     <code>()</code>
 *
 * 2) List of explicitly declared arguments' types, for instance:
 *     <code>(Integer x, String s)</code>
 *
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
        (
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
        {startOfCond = (CloverToken)LT(1);}
        logicalOrExpression
        (   endOfCond:QUESTION
            {if (!constExpr) instrBoolExpr(startOfCond, (CloverToken)endOfCond);}
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
        |   "instanceof" type=typeSpec
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
 *
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
 *
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
            { classCastStart = (CloverToken)LT(1); }
            LPAREN  type=classTypeSpec (BAND type=classOrInterfaceType)* RPAREN
            { classCastEnd = (CloverToken)LT(0); }
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
    /**
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
            startMethodReference = (CloverToken)LT(1);
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
                instrExitMethodReference(expressionEntryEmitter, (CloverToken)LT(0));
            }
        |
            supplementaryPostIncDecPart   // possibly add on a post-increment or post-decrement.
        )
    ;



// the basic element of an expression
primaryExpressionPart
{
  String type = null;
}    :   IDENT
            {
                pushIdentifierToHeadStack(LT(0).getText());
            }
    |   constant
    |   "true"
    |   "false"
    |   "this"
            {
                pushIdentifierToHeadStack(LT(0).getText());
            }
    |   "null"
    |   newExpression
    |   LPAREN! assignmentExpression RPAREN!
    |   "super"
            {
                pushIdentifierToHeadStack(LT(0).getText());
            }
        // look for int.class and int[].class
    |   type=builtInType
        ( LBRACK  RBRACK! )*
        DOT "class"
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
                | "this"
                | "class"
                | newExpression
                | "super" // ClassName.super.field
                )
            // the above line needs a semantic check to make sure "class"
            // is the _last_ qualifier.
        |
            ( LBRACK  RBRACK! )+
            (
                // allow ClassName[].class
                DOT "class"
            |
                // allow constructor reference for arrays, like "SomeType[]::new"
                METHOD_REF "new"
                {
                    // we don't have a method signature here, so well use just a basic name like
                    Parameter[] la = new Parameter[0];
                    MethodSignature methodReferenceSignature = new MethodSignature(
                            LambdaUtil.generateLambdaNameWithId(la, lambdaCounter++), null, null, la, null, new Modifiers());
                    LambdaExpressionEntryEmitter expressionEntryEmitter = instrEnterLambdaExpression(
                            methodReferenceSignature, startMethodReference, startMethodReference,
                            classCastStart, classCastEnd);
                    instrExitLambdaExpression(expressionEntryEmitter, (CloverToken)LT(0));
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
        ( IDENT | "new" )               // e.g. in "foo" / "<String>goo" / "<Integer>new"
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
 *
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
 *
 */
newExpression
{
    CloverToken endOfBlock = null;
    String typeParam = null;
}
    :   "new" (typeParam=typeParameters)? type
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
        AT annotationName=identifier { ident=(CloverToken)LT(0);isSuppressWarnings = instrSuppressWarnings && "SuppressWarnings".equals(annotationName);anno.setName(annotationName);}
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
                CloverToken instrPoint = (CloverToken)lparen;
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
                    ((CloverToken)t).addPreEmitter(suppressWarningsInstr);
                    ((CloverToken)t).addPostEmitter(comma);
                    ((CloverToken)t).addPostEmitter(fallthrough);
                    ((CloverToken)t).addPostEmitter(closeCurly);
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
	CloverToken start = (CloverToken)LT(1);
	CloverToken end = null;
	asString = null;
}
	:
	conditionalExpression { end = (CloverToken)LT(0); asString = TokenListUtil.getNormalisedSequence(start, end); }	
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
            CloverToken t = (CloverToken)LT(0);
            if (isSuppressWarnings) {

                if (seenFallthrough) {
                    // we don't need to set the suppression because one is already present
                    existingFallthroughSuppression = true;
                }
                else {
                    // add "fallthrough" to existing array init
                    if (emitComma) {
                        suppressWarningsInstr = new SimpleEmitter(",");
                        ((CloverToken)t).addPostEmitter(suppressWarningsInstr);
                    }
                    Emitter fallthrough = new SimpleEmitter("\"fallthrough\"");
                    ((CloverToken)t).addPostEmitter(fallthrough);
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


//----------------------------------------------------------------------------
// The Java scanner
//----------------------------------------------------------------------------
class JavaLexer extends Lexer;

options {
    exportVocab=Java;      // call the vocabulary "Java"
    testLiterals=false;    // don't automatically test for literals
    k=4;                   // four characters of lookahead
    charVocabulary='\u0000'..'\uFFFE';
    // without inlining some bitset tests, couldn't do unicode;
    // I need to make ANTLR generate smaller bitsets; see
    // bottom of JavaLexer.java
    codeGenBitsetTestThreshold=30;
}

{
    private boolean noncomment = false;
    private int linecount = 1; // ##HACK - to cope with 1 line files that have no new line
    private int ncLinecount = 0;
    private JavaInstrumentationConfig mConfig;

    public JavaLexer(Reader in, JavaInstrumentationConfig aCfg) {
        this(new CharBuffer(in));
        setTabSize(1);
        mConfig = aCfg;
    }
    
	protected void nc() {
		noncomment = true;
	}

	public void newline() {
		linecount++;
		if (noncomment) {
			ncLinecount++;
			noncomment = false;
		}
		super.newline();
	}

	public int getLineCount() {
		return linecount;
	}

	public int getNCLineCount() {
		return ncLinecount;
	}
}


// OPERATORS
QUESTION        :   '?'     {nc();};
LPAREN          :   '('     {nc();};
RPAREN          :   ')'     {nc();};
LBRACK          :   '['     {nc();};
RBRACK          :   ']'     {nc();};
LCURLY          :   '{'     {nc();};
RCURLY          :   '}'     {nc();};
COLON           :   ':'     {nc();};
METHOD_REF      :   "::"    {nc();};  // method reference operator for lambdas, for instance 'Math::abs'
COMMA           :   ','     {nc();};
//DOT           :   '.'     {nc();};
ASSIGN          :   '='     {nc();};
EQUAL           :   "=="    {nc();};
LNOT            :   '!'     {nc();};
BNOT            :   '~'     {nc();};
NOT_EQUAL       :   "!="    {nc();};
DIV             :   '/'     {nc();};
DIV_ASSIGN      :   "/="    {nc();};
PLUS            :   '+'     {nc();};
PLUS_ASSIGN     :   "+="    {nc();};
INC             :   "++"    {nc();};
MINUS           :   '-'     {nc();};
MINUS_ASSIGN    :   "-="    {nc();};
DEC             :   "--"    {nc();};
STAR            :   '*'     {nc();};
STAR_ASSIGN     :   "*="    {nc();};
MOD             :   '%'     {nc();};
MOD_ASSIGN      :   "%="    {nc();};
SR              :   ">>"    {nc();};
SR_ASSIGN       :   ">>="   {nc();};
BSR             :   ">>>"   {nc();};
BSR_ASSIGN      :   ">>>="  {nc();};
GE              :   ">="    {nc();};
GT              :   ">"     {nc();};
SL              :   "<<"    {nc();};
SL_ASSIGN       :   "<<="   {nc();};
LE              :   "<="    {nc();};
LT              :   '<'     {nc();};
BXOR            :   '^'     {nc();};
BXOR_ASSIGN     :   "^="    {nc();};
BOR             :   '|'     {nc();};
BOR_ASSIGN      :   "|="    {nc();};
LOR             :   "||"    {nc();};
BAND            :   '&'     {nc();};
BAND_ASSIGN     :   "&="    {nc();};
LAND            :   "&&"    {nc();};
SEMI            :   ';'     {nc();};
LAMBDA          :   "->"    {nc();};   // lambda arrow
//ELLIPSIS        :   "..."   ;
// annotation token
AT              :    '@'    {nc();};

// Whitespace -- ignored
WS  :   (   ' '
        |   '\t'
        |   '\f'
            // handle newlines
        |   (   options {generateAmbigWarnings=false;}
            :   "\r\n"  // Evil DOS
            |   '\r'    // Macintosh
            |   '\n'    // Unix (the right way)
            )
            { newline(); }
        )+
    ;

// Single-line comments
SL_COMMENT
    :   "//"
        (~('\n'|'\r') )*
        // don't match a newline always, becuase there might not be one!
        //('\n'|'\r'('\n')? {System.out.println("match point 2");} )
        //{newline();}
    ;

// multiple-line comments
ML_COMMENT
    :   "/*"
        (   /*  '\r' '\n' can be matched in one alternative or by matching
                '\r' in one iteration and '\n' in another.  I am trying to
                handle any flavor of newline that comes in, but the language
                that allows both "\r\n" and "\r" and "\n" to all be valid
                newline is ambiguous.  Consequently, the resulting grammar
                must be ambiguous.  I'm shutting this warning off.
             */
            options {
                generateAmbigWarnings=false;
            }
        :
            { LA(2)!='/' }? '*'
        |   '\r' '\n'       {newline();}
        |   '\r'            {newline();}
        |   '\n'            {newline();}
        |   ~('*'|'\n'|'\r')
        )*
        "*/"
    ;


/**
 * Character literals. In single quotes we can have:
 *  - a character escaped by a backslash, such as '\t', '\u0000'
 *  - any other character except: CR, LF, single quote, non-escaped backslash
 */
CHAR_LITERAL
    : {nc();}   '\'' ( ESC | ~( '\'' | '\\' | '\n' | '\r') ) '\''
    ;

/**
 * String literals. In double quotes we can have:
 *  - a character escaped by a backslash, such as '\t', '\u0000'
 *  - any other character except: CR, LF, double quote, non-escaped backslash
 *
 * In the text blocks, we can have any characters except for the three double
 * quotes not led with a backslash.
 */
STRING_LITERAL
    : STRING_LITERAL_SINGLE_LINE
    | STRING_LITERAL_TEXT_BLOCK
    ;

protected STRING_LITERAL_SINGLE_LINE
    : {nc();}   '"' ( ESC | ~( '"' | '\\' | '\n' | '\r') )* '"'
    ;

protected STRING_LITERAL_TEXT_BLOCK
    : {nc();} '"' '"' '"' ( '\r' | '\n' ) 
        (   (  BACKSLASH   '"' '"'  '"' ) =>  BACKSLASH   '"' '"' '"'
          | ( (BACKSLASH)? '"' '"' ~'"' ) => (BACKSLASH)? '"' '"'
          | ( (BACKSLASH)? '"'     ~'"' ) => (BACKSLASH)? '"'
          | '\r' '\n'       {newline();}
          | '\r'            {newline();}
          | '\n'            {newline();}
          | ESC
          | ~('\n'|'\r'|'"')
        )*
        '"' '"' '"'
    ;

// escape sequence -- note that this is protected; it can only be called
//   from another lexer rule -- it will not ever directly return a token to
//   the parser
// There are various ambiguities hushed in this rule.  The optional
// '0'...'9' digit matches should be matched here rather than letting
// them go back to STRING_LITERAL to be matched.  ANTLR does the
// right thing by matching immediately; hence, it's ok to shut off
// the FOLLOW ambig warnings.
protected
ESC
    :   BACKSLASH
        (   'n'
        |   'r'
        |   't'
        |   'b'
        |   'f'
        |   '"'
        |   '\''
        |   BACKSLASH
        |   ('u')+ HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
        |   ('0'..'3')
            (
                options {
                    warnWhenFollowAmbig = false;
                }
            :   ('0'..'7')
                (
                    options {
                        warnWhenFollowAmbig = false;
                    }
                :   '0'..'7'
                )?
            )?
        |   ('4'..'7')
            (
                options {
                    warnWhenFollowAmbig = false;
                }
            :   ('0'..'9')
            )?
        )
    ;

/**
 * Backslash can be used for escaping subsequent character. It can be written directly as '\' or using
 * it's unicode equivalent as '\u005C' (yes, by using another backslash before 'u' :-) ).
 *
 * Therefore in the UnicodeDecodingReader we treat it specially and do not translate its unicode sequence to raw UTF.
 * Thus, the JavaLexer shall be able to handle both forms.
 */
protected BACKSLASH
    :
        ( '\\' ('u')+ '0' '0' '5' ('c' | 'C') ) =>
        '\\' ('u')+ '0' '0' '5' ('c' | 'C')
    |
        '\\'
    ;


// a dummy rule to force vocabulary to be all characters (except special
//   ones that ANTLR uses internally (0 to 2)
protected
VOCAB
    :   '\3'..'\377'
    ;

/**
 * An identifier.  Note that testLiterals is set to true!  This means that after we match the rule, we look in the
 * literals table to see if it's a literal or really an identifer
 */
IDENT
options { testLiterals=true; }
    :
        { nc(); } IdentifierStart (IdentifierPart)*
    ;

// a complete rewrite of the overly spaghettied NUM_INT rule, to support hex floats
// inspired by
// http://fisheye1.cenqua.com/browse/checkstyle/checkstyle/src/checkstyle/com/puppycrawl/tools/checkstyle/grammars/java.g?r1=1.11&r2=1.12
NUM_INT
{
    nc();
}
      :   (ELLIPSIS)=>ELLIPSIS {_ttype = ELLIPSIS;}
      |   (DOT)=>DOT {_ttype = DOT;}
      |   (DOUBLE_LITERAL)=>DOUBLE_LITERAL {_ttype = NUM_DOUBLE;}
      |   (FLOAT_LITERAL)=>FLOAT_LITERAL {_ttype = NUM_FLOAT;}
      |   (HEX_FLOAT_LITERAL)=>HEX_FLOAT_LITERAL
      |   (LONG_LITERAL)=>LONG_LITERAL {_ttype = NUM_LONG;}
      |   (INT_LITERAL)=>INT_LITERAL {_ttype = NUM_INT;}
      ;

protected INT_LITERAL
    :   (    '0'
             (  ('x'|'X') HEX_INT
             |  ('b'|'B') BIN_INT
             |  (DEC_DIGIT | '_')*
             )
        // non-zero decimal
        |	('1'..'9') (DEC_DIGIT | '_')*
        )
    ;

protected LONG_LITERAL
    :    (   '0'
             (  ('x'|'X') HEX_INT
             |  ('b'|'B') BIN_INT
             |  (DEC_DIGIT | '_')*
             )
         // non-zero decimal
         |    ('1'..'9') (DEC_DIGIT | '_')*
         )
         // long signifier
         ('l'|'L')
    ;

protected FLOAT_LITERAL
    :   (
            ((DEC_INT)? '.')=>
            (   DEC_INT '.' (DEC_INT)?
            |   '.' DEC_INT
            )
            (EXPONENT)? ('f'|'F')?
        |
            DEC_INT ((EXPONENT ('f'|'F')?) | ('f'|'F'))
        )
    ;

protected DOUBLE_LITERAL
    :   (
            ((DEC_INT)? '.')=>
            (   DEC_INT '.' (DEC_INT)?
            |   '.' DEC_INT
            )
        |
            DEC_INT
        )
        (EXPONENT)? ('d'|'D')
    ;

protected HEX_FLOAT_LITERAL
    :   '0' ('x'|'X')
        (
            ((HEX_INT)? '.')=>
            (   HEX_INT '.' (HEX_INT)?
            |   '.' HEX_INT
            )
        |
            HEX_INT
        )
        BINARY_EXPONENT
        {_ttype = NUM_FLOAT;}
        (
            ('f'|'F')?
        |
            ('d'|'D') {_ttype = NUM_DOUBLE;}
        )
    ;

protected DEC_DIGIT
    :   '0'..'9'
    ;

protected BIN_DIGIT
    :   '0'..'1'
    ;

protected HEX_DIGIT
    :   (DEC_DIGIT|'A'..'F'|'a'..'f')
    ;

protected DEC_INT
    :   DEC_DIGIT (DEC_DIGIT | '_')*
    ;

protected HEX_INT
    :   HEX_DIGIT (HEX_DIGIT | '_')*
    ;

protected BIN_INT
    :   BIN_DIGIT (BIN_DIGIT | '_')*
    ;

protected ELLIPSIS
    :   '.''.''.'
    ;

protected DOT
    :   '.'
    ;

protected FLOAT_SUFFIX
	:	'f'|'F'|'d'|'D'
	;

protected EXPONENT
	:	('e'|'E') ('+'|'-')? DEC_INT
	;

protected BINARY_EXPONENT
    :   ('p'|'P') ('+'|'-')? DEC_INT
    ;



// ========================================================================================================
// EXCERPT FROM THE http://openjdk.java.net/projects/compiler-grammar/antlrworks/Java.g
// ========================================================================================================

/*
 [The "BSD licence"]
 Copyright (c) 2007-2008 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

protected IdentifierStart
    :   '\u0024'
    |   '\u0041'..'\u005a'
    |   '\u005f'
    |   '\u0061'..'\u007a'
    |   '\u00a2'..'\u00a5'
    |   '\u00aa'
    |   '\u00b5'
    |   '\u00ba'
    |   '\u00c0'..'\u00d6'
    |   '\u00d8'..'\u00f6'
    |   '\u00f8'..'\u0236'
    |   '\u0250'..'\u02c1'
    |   '\u02c6'..'\u02d1'
    |   '\u02e0'..'\u02e4'
    |   '\u02ee'
    |   '\u037a'
    |   '\u0386'
    |   '\u0388'..'\u038a'
    |   '\u038c'
    |   '\u038e'..'\u03a1'
    |   '\u03a3'..'\u03ce'
    |   '\u03d0'..'\u03f5'
    |   '\u03f7'..'\u03fb'
    |   '\u0400'..'\u0481'
    |   '\u048a'..'\u04ce'
    |   '\u04d0'..'\u04f5'
    |   '\u04f8'..'\u04f9'
    |   '\u0500'..'\u050f'
    |   '\u0531'..'\u0556'
    |   '\u0559'
    |   '\u0561'..'\u0587'
    |   '\u05d0'..'\u05ea'
    |   '\u05f0'..'\u05f2'
    |   '\u0621'..'\u063a'
    |   '\u0640'..'\u064a'
    |   '\u066e'..'\u066f'
    |   '\u0671'..'\u06d3'
    |   '\u06d5'
    |   '\u06e5'..'\u06e6'
    |   '\u06ee'..'\u06ef'
    |   '\u06fa'..'\u06fc'
    |   '\u06ff'
    |   '\u0710'
    |   '\u0712'..'\u072f'
    |   '\u074d'..'\u074f'
    |   '\u0780'..'\u07a5'
    |   '\u07b1'
    |   '\u0904'..'\u0939'
    |   '\u093d'
    |   '\u0950'
    |   '\u0958'..'\u0961'
    |   '\u0985'..'\u098c'
    |   '\u098f'..'\u0990'
    |   '\u0993'..'\u09a8'
    |   '\u09aa'..'\u09b0'
    |   '\u09b2'
    |   '\u09b6'..'\u09b9'
    |   '\u09bd'
    |   '\u09dc'..'\u09dd'
    |   '\u09df'..'\u09e1'
    |   '\u09f0'..'\u09f3'
    |   '\u0a05'..'\u0a0a'
    |   '\u0a0f'..'\u0a10'
    |   '\u0a13'..'\u0a28'
    |   '\u0a2a'..'\u0a30'
    |   '\u0a32'..'\u0a33'
    |   '\u0a35'..'\u0a36'
    |   '\u0a38'..'\u0a39'
    |   '\u0a59'..'\u0a5c'
    |   '\u0a5e'
    |   '\u0a72'..'\u0a74'
    |   '\u0a85'..'\u0a8d'
    |   '\u0a8f'..'\u0a91'
    |   '\u0a93'..'\u0aa8'
    |   '\u0aaa'..'\u0ab0'
    |   '\u0ab2'..'\u0ab3'
    |   '\u0ab5'..'\u0ab9'
    |   '\u0abd'
    |   '\u0ad0'
    |   '\u0ae0'..'\u0ae1'
    |   '\u0af1'
    |   '\u0b05'..'\u0b0c'
    |   '\u0b0f'..'\u0b10'
    |   '\u0b13'..'\u0b28'
    |   '\u0b2a'..'\u0b30'
    |   '\u0b32'..'\u0b33'
    |   '\u0b35'..'\u0b39'
    |   '\u0b3d'
    |   '\u0b5c'..'\u0b5d'
    |   '\u0b5f'..'\u0b61'
    |   '\u0b71'
    |   '\u0b83'
    |   '\u0b85'..'\u0b8a'
    |   '\u0b8e'..'\u0b90'
    |   '\u0b92'..'\u0b95'
    |   '\u0b99'..'\u0b9a'
    |   '\u0b9c'
    |   '\u0b9e'..'\u0b9f'
    |   '\u0ba3'..'\u0ba4'
    |   '\u0ba8'..'\u0baa'
    |   '\u0bae'..'\u0bb5'
    |   '\u0bb7'..'\u0bb9'
    |   '\u0bf9'
    |   '\u0c05'..'\u0c0c'
    |   '\u0c0e'..'\u0c10'
    |   '\u0c12'..'\u0c28'
    |   '\u0c2a'..'\u0c33'
    |   '\u0c35'..'\u0c39'
    |   '\u0c60'..'\u0c61'
    |   '\u0c85'..'\u0c8c'
    |   '\u0c8e'..'\u0c90'
    |   '\u0c92'..'\u0ca8'
    |   '\u0caa'..'\u0cb3'
    |   '\u0cb5'..'\u0cb9'
    |   '\u0cbd'
    |   '\u0cde'
    |   '\u0ce0'..'\u0ce1'
    |   '\u0d05'..'\u0d0c'
    |   '\u0d0e'..'\u0d10'
    |   '\u0d12'..'\u0d28'
    |   '\u0d2a'..'\u0d39'
    |   '\u0d60'..'\u0d61'
    |   '\u0d85'..'\u0d96'
    |   '\u0d9a'..'\u0db1'
    |   '\u0db3'..'\u0dbb'
    |   '\u0dbd'
    |   '\u0dc0'..'\u0dc6'
    |   '\u0e01'..'\u0e30'
    |   '\u0e32'..'\u0e33'
    |   '\u0e3f'..'\u0e46'
    |   '\u0e81'..'\u0e82'
    |   '\u0e84'
    |   '\u0e87'..'\u0e88'
    |   '\u0e8a'
    |   '\u0e8d'
    |   '\u0e94'..'\u0e97'
    |   '\u0e99'..'\u0e9f'
    |   '\u0ea1'..'\u0ea3'
    |   '\u0ea5'
    |   '\u0ea7'
    |   '\u0eaa'..'\u0eab'
    |   '\u0ead'..'\u0eb0'
    |   '\u0eb2'..'\u0eb3'
    |   '\u0ebd'
    |   '\u0ec0'..'\u0ec4'
    |   '\u0ec6'
    |   '\u0edc'..'\u0edd'
    |   '\u0f00'
    |   '\u0f40'..'\u0f47'
    |   '\u0f49'..'\u0f6a'
    |   '\u0f88'..'\u0f8b'
    |   '\u1000'..'\u1021'
    |   '\u1023'..'\u1027'
    |   '\u1029'..'\u102a'
    |   '\u1050'..'\u1055'
    |   '\u10a0'..'\u10c5'
    |   '\u10d0'..'\u10f8'
    |   '\u1100'..'\u1159'
    |   '\u115f'..'\u11a2'
    |   '\u11a8'..'\u11f9'
    |   '\u1200'..'\u1206'
    |   '\u1208'..'\u1246'
    |   '\u1248'
    |   '\u124a'..'\u124d'
    |   '\u1250'..'\u1256'
    |   '\u1258'
    |   '\u125a'..'\u125d'
    |   '\u1260'..'\u1286'
    |   '\u1288'
    |   '\u128a'..'\u128d'
    |   '\u1290'..'\u12ae'
    |   '\u12b0'
    |   '\u12b2'..'\u12b5'
    |   '\u12b8'..'\u12be'
    |   '\u12c0'
    |   '\u12c2'..'\u12c5'
    |   '\u12c8'..'\u12ce'
    |   '\u12d0'..'\u12d6'
    |   '\u12d8'..'\u12ee'
    |   '\u12f0'..'\u130e'
    |   '\u1310'
    |   '\u1312'..'\u1315'
    |   '\u1318'..'\u131e'
    |   '\u1320'..'\u1346'
    |   '\u1348'..'\u135a'
    |   '\u13a0'..'\u13f4'
    |   '\u1401'..'\u166c'
    |   '\u166f'..'\u1676'
    |   '\u1681'..'\u169a'
    |   '\u16a0'..'\u16ea'
    |   '\u16ee'..'\u16f0'
    |   '\u1700'..'\u170c'
    |   '\u170e'..'\u1711'
    |   '\u1720'..'\u1731'
    |   '\u1740'..'\u1751'
    |   '\u1760'..'\u176c'
    |   '\u176e'..'\u1770'
    |   '\u1780'..'\u17b3'
    |   '\u17d7'
    |   '\u17db'..'\u17dc'
    |   '\u1820'..'\u1877'
    |   '\u1880'..'\u18a8'
    |   '\u1900'..'\u191c'
    |   '\u1950'..'\u196d'
    |   '\u1970'..'\u1974'
    |   '\u1d00'..'\u1d6b'
    |   '\u1e00'..'\u1e9b'
    |   '\u1ea0'..'\u1ef9'
    |   '\u1f00'..'\u1f15'
    |   '\u1f18'..'\u1f1d'
    |   '\u1f20'..'\u1f45'
    |   '\u1f48'..'\u1f4d'
    |   '\u1f50'..'\u1f57'
    |   '\u1f59'
    |   '\u1f5b'
    |   '\u1f5d'
    |   '\u1f5f'..'\u1f7d'
    |   '\u1f80'..'\u1fb4'
    |   '\u1fb6'..'\u1fbc'
    |   '\u1fbe'
    |   '\u1fc2'..'\u1fc4'
    |   '\u1fc6'..'\u1fcc'
    |   '\u1fd0'..'\u1fd3'
    |   '\u1fd6'..'\u1fdb'
    |   '\u1fe0'..'\u1fec'
    |   '\u1ff2'..'\u1ff4'
    |   '\u1ff6'..'\u1ffc'
    |   '\u203f'..'\u2040'
    |   '\u2054'
    |   '\u2071'
    |   '\u207f'
    |   '\u20a0'..'\u20b1'
    |   '\u2102'
    |   '\u2107'
    |   '\u210a'..'\u2113'
    |   '\u2115'
    |   '\u2119'..'\u211d'
    |   '\u2124'
    |   '\u2126'
    |   '\u2128'
    |   '\u212a'..'\u212d'
    |   '\u212f'..'\u2131'
    |   '\u2133'..'\u2139'
    |   '\u213d'..'\u213f'
    |   '\u2145'..'\u2149'
    |   '\u2160'..'\u2183'
    |   '\u3005'..'\u3007'
    |   '\u3021'..'\u3029'
    |   '\u3031'..'\u3035'
    |   '\u3038'..'\u303c'
    |   '\u3041'..'\u3096'
    |   '\u309d'..'\u309f'
    |   '\u30a1'..'\u30ff'
    |   '\u3105'..'\u312c'
    |   '\u3131'..'\u318e'
    |   '\u31a0'..'\u31b7'
    |   '\u31f0'..'\u31ff'
    |   '\u3400'..'\u4db5'
    |   '\u4e00'..'\u9fa5'
    |   '\ua000'..'\ua48c'
    |   '\uac00'..'\ud7a3'
    |   '\uf900'..'\ufa2d'
    |   '\ufa30'..'\ufa6a'
    |   '\ufb00'..'\ufb06'
    |   '\ufb13'..'\ufb17'
    |   '\ufb1d'
    |   '\ufb1f'..'\ufb28'
    |   '\ufb2a'..'\ufb36'
    |   '\ufb38'..'\ufb3c'
    |   '\ufb3e'
    |   '\ufb40'..'\ufb41'
    |   '\ufb43'..'\ufb44'
    |   '\ufb46'..'\ufbb1'
    |   '\ufbd3'..'\ufd3d'
    |   '\ufd50'..'\ufd8f'
    |   '\ufd92'..'\ufdc7'
    |   '\ufdf0'..'\ufdfc'
    |   '\ufe33'..'\ufe34'
    |   '\ufe4d'..'\ufe4f'
    |   '\ufe69'
    |   '\ufe70'..'\ufe74'
    |   '\ufe76'..'\ufefc'
    |   '\uff04'
    |   '\uff21'..'\uff3a'
    |   '\uff3f'
    |   '\uff41'..'\uff5a'
    |   '\uff65'..'\uffbe'
    |   '\uffc2'..'\uffc7'
    |   '\uffca'..'\uffcf'
    |   '\uffd2'..'\uffd7'
    |   '\uffda'..'\uffdc'
    |   '\uffe0'..'\uffe1'
    |   '\uffe5'..'\uffe6'
    |   ('\ud800'..'\udbff') ('\udc00'..'\udfff')
    ;

protected IdentifierPart
    :   '\u0000'..'\u0008'
    |   '\u000e'..'\u001b'
    |   '\u0024'
    |   '\u0030'..'\u0039'
    |   '\u0041'..'\u005a'
    |   '\u005f'
    |   '\u0061'..'\u007a'
    |   '\u007f'..'\u009f'
    |   '\u00a2'..'\u00a5'
    |   '\u00aa'
    |   '\u00ad'
    |   '\u00b5'
    |   '\u00ba'
    |   '\u00c0'..'\u00d6'
    |   '\u00d8'..'\u00f6'
    |   '\u00f8'..'\u0236'
    |   '\u0250'..'\u02c1'
    |   '\u02c6'..'\u02d1'
    |   '\u02e0'..'\u02e4'
    |   '\u02ee'
    |   '\u0300'..'\u0357'
    |   '\u035d'..'\u036f'
    |   '\u037a'
    |   '\u0386'
    |   '\u0388'..'\u038a'
    |   '\u038c'
    |   '\u038e'..'\u03a1'
    |   '\u03a3'..'\u03ce'
    |   '\u03d0'..'\u03f5'
    |   '\u03f7'..'\u03fb'
    |   '\u0400'..'\u0481'
    |   '\u0483'..'\u0486'
    |   '\u048a'..'\u04ce'
    |   '\u04d0'..'\u04f5'
    |   '\u04f8'..'\u04f9'
    |   '\u0500'..'\u050f'
    |   '\u0531'..'\u0556'
    |   '\u0559'
    |   '\u0561'..'\u0587'
    |   '\u0591'..'\u05a1'
    |   '\u05a3'..'\u05b9'
    |   '\u05bb'..'\u05bd'
    |   '\u05bf'
    |   '\u05c1'..'\u05c2'
    |   '\u05c4'
    |   '\u05d0'..'\u05ea'
    |   '\u05f0'..'\u05f2'
    |   '\u0600'..'\u0603'
    |   '\u0610'..'\u0615'
    |   '\u0621'..'\u063a'
    |   '\u0640'..'\u0658'
    |   '\u0660'..'\u0669'
    |   '\u066e'..'\u06d3'
    |   '\u06d5'..'\u06dd'
    |   '\u06df'..'\u06e8'
    |   '\u06ea'..'\u06fc'
    |   '\u06ff'
    |   '\u070f'..'\u074a'
    |   '\u074d'..'\u074f'
    |   '\u0780'..'\u07b1'
    |   '\u0901'..'\u0939'
    |   '\u093c'..'\u094d'
    |   '\u0950'..'\u0954'
    |   '\u0958'..'\u0963'
    |   '\u0966'..'\u096f'
    |   '\u0981'..'\u0983'
    |   '\u0985'..'\u098c'
    |   '\u098f'..'\u0990'
    |   '\u0993'..'\u09a8'
    |   '\u09aa'..'\u09b0'
    |   '\u09b2'
    |   '\u09b6'..'\u09b9'
    |   '\u09bc'..'\u09c4'
    |   '\u09c7'..'\u09c8'
    |   '\u09cb'..'\u09cd'
    |   '\u09d7'
    |   '\u09dc'..'\u09dd'
    |   '\u09df'..'\u09e3'
    |   '\u09e6'..'\u09f3'
    |   '\u0a01'..'\u0a03'
    |   '\u0a05'..'\u0a0a'
    |   '\u0a0f'..'\u0a10'
    |   '\u0a13'..'\u0a28'
    |   '\u0a2a'..'\u0a30'
    |   '\u0a32'..'\u0a33'
    |   '\u0a35'..'\u0a36'
    |   '\u0a38'..'\u0a39'
    |   '\u0a3c'
    |   '\u0a3e'..'\u0a42'
    |   '\u0a47'..'\u0a48'
    |   '\u0a4b'..'\u0a4d'
    |   '\u0a59'..'\u0a5c'
    |   '\u0a5e'
    |   '\u0a66'..'\u0a74'
    |   '\u0a81'..'\u0a83'
    |   '\u0a85'..'\u0a8d'
    |   '\u0a8f'..'\u0a91'
    |   '\u0a93'..'\u0aa8'
    |   '\u0aaa'..'\u0ab0'
    |   '\u0ab2'..'\u0ab3'
    |   '\u0ab5'..'\u0ab9'
    |   '\u0abc'..'\u0ac5'
    |   '\u0ac7'..'\u0ac9'
    |   '\u0acb'..'\u0acd'
    |   '\u0ad0'
    |   '\u0ae0'..'\u0ae3'
    |   '\u0ae6'..'\u0aef'
    |   '\u0af1'
    |   '\u0b01'..'\u0b03'
    |   '\u0b05'..'\u0b0c'
    |   '\u0b0f'..'\u0b10'
    |   '\u0b13'..'\u0b28'
    |   '\u0b2a'..'\u0b30'
    |   '\u0b32'..'\u0b33'
    |   '\u0b35'..'\u0b39'
    |   '\u0b3c'..'\u0b43'
    |   '\u0b47'..'\u0b48'
    |   '\u0b4b'..'\u0b4d'
    |   '\u0b56'..'\u0b57'
    |   '\u0b5c'..'\u0b5d'
    |   '\u0b5f'..'\u0b61'
    |   '\u0b66'..'\u0b6f'
    |   '\u0b71'
    |   '\u0b82'..'\u0b83'
    |   '\u0b85'..'\u0b8a'
    |   '\u0b8e'..'\u0b90'
    |   '\u0b92'..'\u0b95'
    |   '\u0b99'..'\u0b9a'
    |   '\u0b9c'
    |   '\u0b9e'..'\u0b9f'
    |   '\u0ba3'..'\u0ba4'
    |   '\u0ba8'..'\u0baa'
    |   '\u0bae'..'\u0bb5'
    |   '\u0bb7'..'\u0bb9'
    |   '\u0bbe'..'\u0bc2'
    |   '\u0bc6'..'\u0bc8'
    |   '\u0bca'..'\u0bcd'
    |   '\u0bd7'
    |   '\u0be7'..'\u0bef'
    |   '\u0bf9'
    |   '\u0c01'..'\u0c03'
    |   '\u0c05'..'\u0c0c'
    |   '\u0c0e'..'\u0c10'
    |   '\u0c12'..'\u0c28'
    |   '\u0c2a'..'\u0c33'
    |   '\u0c35'..'\u0c39'
    |   '\u0c3e'..'\u0c44'
    |   '\u0c46'..'\u0c48'
    |   '\u0c4a'..'\u0c4d'
    |   '\u0c55'..'\u0c56'
    |   '\u0c60'..'\u0c61'
    |   '\u0c66'..'\u0c6f'
    |   '\u0c82'..'\u0c83'
    |   '\u0c85'..'\u0c8c'
    |   '\u0c8e'..'\u0c90'
    |   '\u0c92'..'\u0ca8'
    |   '\u0caa'..'\u0cb3'
    |   '\u0cb5'..'\u0cb9'
    |   '\u0cbc'..'\u0cc4'
    |   '\u0cc6'..'\u0cc8'
    |   '\u0cca'..'\u0ccd'
    |   '\u0cd5'..'\u0cd6'
    |   '\u0cde'
    |   '\u0ce0'..'\u0ce1'
    |   '\u0ce6'..'\u0cef'
    |   '\u0d02'..'\u0d03'
    |   '\u0d05'..'\u0d0c'
    |   '\u0d0e'..'\u0d10'
    |   '\u0d12'..'\u0d28'
    |   '\u0d2a'..'\u0d39'
    |   '\u0d3e'..'\u0d43'
    |   '\u0d46'..'\u0d48'
    |   '\u0d4a'..'\u0d4d'
    |   '\u0d57'
    |   '\u0d60'..'\u0d61'
    |   '\u0d66'..'\u0d6f'
    |   '\u0d82'..'\u0d83'
    |   '\u0d85'..'\u0d96'
    |   '\u0d9a'..'\u0db1'
    |   '\u0db3'..'\u0dbb'
    |   '\u0dbd'
    |   '\u0dc0'..'\u0dc6'
    |   '\u0dca'
    |   '\u0dcf'..'\u0dd4'
    |   '\u0dd6'
    |   '\u0dd8'..'\u0ddf'
    |   '\u0df2'..'\u0df3'
    |   '\u0e01'..'\u0e3a'
    |   '\u0e3f'..'\u0e4e'
    |   '\u0e50'..'\u0e59'
    |   '\u0e81'..'\u0e82'
    |   '\u0e84'
    |   '\u0e87'..'\u0e88'
    |   '\u0e8a'
    |   '\u0e8d'
    |   '\u0e94'..'\u0e97'
    |   '\u0e99'..'\u0e9f'
    |   '\u0ea1'..'\u0ea3'
    |   '\u0ea5'
    |   '\u0ea7'
    |   '\u0eaa'..'\u0eab'
    |   '\u0ead'..'\u0eb9'
    |   '\u0ebb'..'\u0ebd'
    |   '\u0ec0'..'\u0ec4'
    |   '\u0ec6'
    |   '\u0ec8'..'\u0ecd'
    |   '\u0ed0'..'\u0ed9'
    |   '\u0edc'..'\u0edd'
    |   '\u0f00'
    |   '\u0f18'..'\u0f19'
    |   '\u0f20'..'\u0f29'
    |   '\u0f35'
    |   '\u0f37'
    |   '\u0f39'
    |   '\u0f3e'..'\u0f47'
    |   '\u0f49'..'\u0f6a'
    |   '\u0f71'..'\u0f84'
    |   '\u0f86'..'\u0f8b'
    |   '\u0f90'..'\u0f97'
    |   '\u0f99'..'\u0fbc'
    |   '\u0fc6'
    |   '\u1000'..'\u1021'
    |   '\u1023'..'\u1027'
    |   '\u1029'..'\u102a'
    |   '\u102c'..'\u1032'
    |   '\u1036'..'\u1039'
    |   '\u1040'..'\u1049'
    |   '\u1050'..'\u1059'
    |   '\u10a0'..'\u10c5'
    |   '\u10d0'..'\u10f8'
    |   '\u1100'..'\u1159'
    |   '\u115f'..'\u11a2'
    |   '\u11a8'..'\u11f9'
    |   '\u1200'..'\u1206'
    |   '\u1208'..'\u1246'
    |   '\u1248'
    |   '\u124a'..'\u124d'
    |   '\u1250'..'\u1256'
    |   '\u1258'
    |   '\u125a'..'\u125d'
    |   '\u1260'..'\u1286'
    |   '\u1288'
    |   '\u128a'..'\u128d'
    |   '\u1290'..'\u12ae'
    |   '\u12b0'
    |   '\u12b2'..'\u12b5'
    |   '\u12b8'..'\u12be'
    |   '\u12c0'
    |   '\u12c2'..'\u12c5'
    |   '\u12c8'..'\u12ce'
    |   '\u12d0'..'\u12d6'
    |   '\u12d8'..'\u12ee'
    |   '\u12f0'..'\u130e'
    |   '\u1310'
    |   '\u1312'..'\u1315'
    |   '\u1318'..'\u131e'
    |   '\u1320'..'\u1346'
    |   '\u1348'..'\u135a'
    |   '\u1369'..'\u1371'
    |   '\u13a0'..'\u13f4'
    |   '\u1401'..'\u166c'
    |   '\u166f'..'\u1676'
    |   '\u1681'..'\u169a'
    |   '\u16a0'..'\u16ea'
    |   '\u16ee'..'\u16f0'
    |   '\u1700'..'\u170c'
    |   '\u170e'..'\u1714'
    |   '\u1720'..'\u1734'
    |   '\u1740'..'\u1753'
    |   '\u1760'..'\u176c'
    |   '\u176e'..'\u1770'
    |   '\u1772'..'\u1773'
    |   '\u1780'..'\u17d3'
    |   '\u17d7'
    |   '\u17db'..'\u17dd'
    |   '\u17e0'..'\u17e9'
    |   '\u180b'..'\u180d'
    |   '\u1810'..'\u1819'
    |   '\u1820'..'\u1877'
    |   '\u1880'..'\u18a9'
    |   '\u1900'..'\u191c'
    |   '\u1920'..'\u192b'
    |   '\u1930'..'\u193b'
    |   '\u1946'..'\u196d'
    |   '\u1970'..'\u1974'
    |   '\u1d00'..'\u1d6b'
    |   '\u1e00'..'\u1e9b'
    |   '\u1ea0'..'\u1ef9'
    |   '\u1f00'..'\u1f15'
    |   '\u1f18'..'\u1f1d'
    |   '\u1f20'..'\u1f45'
    |   '\u1f48'..'\u1f4d'
    |   '\u1f50'..'\u1f57'
    |   '\u1f59'
    |   '\u1f5b'
    |   '\u1f5d'
    |   '\u1f5f'..'\u1f7d'
    |   '\u1f80'..'\u1fb4'
    |   '\u1fb6'..'\u1fbc'
    |   '\u1fbe'
    |   '\u1fc2'..'\u1fc4'
    |   '\u1fc6'..'\u1fcc'
    |   '\u1fd0'..'\u1fd3'
    |   '\u1fd6'..'\u1fdb'
    |   '\u1fe0'..'\u1fec'
    |   '\u1ff2'..'\u1ff4'
    |   '\u1ff6'..'\u1ffc'
    |   '\u200c'..'\u200f'
    |   '\u202a'..'\u202e'
    |   '\u203f'..'\u2040'
    |   '\u2054'
    |   '\u2060'..'\u2063'
    |   '\u206a'..'\u206f'
    |   '\u2071'
    |   '\u207f'
    |   '\u20a0'..'\u20b1'
    |   '\u20d0'..'\u20dc'
    |   '\u20e1'
    |   '\u20e5'..'\u20ea'
    |   '\u2102'
    |   '\u2107'
    |   '\u210a'..'\u2113'
    |   '\u2115'
    |   '\u2119'..'\u211d'
    |   '\u2124'
    |   '\u2126'
    |   '\u2128'
    |   '\u212a'..'\u212d'
    |   '\u212f'..'\u2131'
    |   '\u2133'..'\u2139'
    |   '\u213d'..'\u213f'
    |   '\u2145'..'\u2149'
    |   '\u2160'..'\u2183'
    |   '\u3005'..'\u3007'
    |   '\u3021'..'\u302f'
    |   '\u3031'..'\u3035'
    |   '\u3038'..'\u303c'
    |   '\u3041'..'\u3096'
    |   '\u3099'..'\u309a'
    |   '\u309d'..'\u309f'
    |   '\u30a1'..'\u30ff'
    |   '\u3105'..'\u312c'
    |   '\u3131'..'\u318e'
    |   '\u31a0'..'\u31b7'
    |   '\u31f0'..'\u31ff'
    |   '\u3400'..'\u4db5'
    |   '\u4e00'..'\u9fa5'
    |   '\ua000'..'\ua48c'
    |   '\uac00'..'\ud7a3'
    |   '\uf900'..'\ufa2d'
    |   '\ufa30'..'\ufa6a'
    |   '\ufb00'..'\ufb06'
    |   '\ufb13'..'\ufb17'
    |   '\ufb1d'..'\ufb28'
    |   '\ufb2a'..'\ufb36'
    |   '\ufb38'..'\ufb3c'
    |   '\ufb3e'
    |   '\ufb40'..'\ufb41'
    |   '\ufb43'..'\ufb44'
    |   '\ufb46'..'\ufbb1'
    |   '\ufbd3'..'\ufd3d'
    |   '\ufd50'..'\ufd8f'
    |   '\ufd92'..'\ufdc7'
    |   '\ufdf0'..'\ufdfc'
    |   '\ufe00'..'\ufe0f'
    |   '\ufe20'..'\ufe23'
    |   '\ufe33'..'\ufe34'
    |   '\ufe4d'..'\ufe4f'
    |   '\ufe69'
    |   '\ufe70'..'\ufe74'
    |   '\ufe76'..'\ufefc'
    |   '\ufeff'
    |   '\uff04'
    |   '\uff10'..'\uff19'
    |   '\uff21'..'\uff3a'
    |   '\uff3f'
    |   '\uff41'..'\uff5a'
    |   '\uff65'..'\uffbe'
    |   '\uffc2'..'\uffc7'
    |   '\uffca'..'\uffcf'
    |   '\uffd2'..'\uffd7'
    |   '\uffda'..'\uffdc'
    |   '\uffe0'..'\uffe1'
    |   '\uffe5'..'\uffe6'
    |   '\ufff9'..'\ufffb'
    |   ('\ud800'..'\udbff') ('\udc00'..'\udfff')
    ;

// ========================================================================================================
// END OF THE EXCERPT
// ========================================================================================================
