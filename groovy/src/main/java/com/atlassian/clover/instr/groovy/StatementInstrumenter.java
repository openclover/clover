package com.atlassian.clover.instr.groovy;

import com.atlassian.clover.api.instrumentation.InstrumentationSession;
import com.atlassian.clover.registry.FixedSourceRegion;
import com.atlassian.clover.spi.lang.LanguageConstruct;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.BreakStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;

import java.util.List;

import static clover.com.google.common.collect.Lists.newArrayList;
import static clover.com.google.common.collect.Lists.newLinkedList;

/**
 * Instrumenting statements.
 */
public class StatementInstrumenter extends ClassInstumenter {

    /** Whether we shall instrument statments */
    private final boolean isStatementInstrEnabled;

    public StatementInstrumenter(final InstrumentationSession session, final ClassNode currentClass,
                                 final boolean isStatementInstrEnabled) {
        super(session, currentClass);
        this.isStatementInstrEnabled = isStatementInstrEnabled;
    }

    protected Statement instrumentBlockStatementOrExpressionStatement(VariableScope scope, Statement mysteryMeat) {
        if (mysteryMeat instanceof BlockStatement) {
            return instrumentBlockStatement(mysteryMeat);
        } else if (mysteryMeat instanceof ExpressionStatement && scope != null) {
            return instrumentExpressionStatement(scope, (ExpressionStatement)mysteryMeat);
        } else {
            return mysteryMeat;
        }
    }

    private Statement instrumentExpressionStatement(VariableScope scope, ExpressionStatement expressionStatement) {
        final Statement instrStatement = instrumentStmt(expressionStatement);
        if (instrStatement != null) {
            return new BlockStatement(newArrayList(instrStatement, expressionStatement), scope);
        } else {
            return expressionStatement;
        }
    }

    protected Statement instrumentBlockStatement(Statement maybeBlockStatement) {
        return instrumentBlockStatement(maybeBlockStatement, null, null);
    }

    @SuppressWarnings("unchecked")
    protected Statement instrumentBlockStatement(Statement maybeBlockStatement,
                                                 Statement entryIncStatement,
                                                 VariableScope currentVariableScope) {
        if (maybeBlockStatement instanceof BlockStatement) {
            // method.getCode() usually returns BlockStatement
            return instrumentBlockStatement((BlockStatement)maybeBlockStatement, entryIncStatement);
        } else if (maybeBlockStatement instanceof TryCatchStatement
                && entryIncStatement != null && currentVariableScope != null) {
            // Controllers in Grails have a TryCatchStatement instead of a BlockStatement in method.getCode()
            // We have to wrap try-catch inside a block and add Clover's recording of a method entry
            final BlockStatement rewritten = new BlockStatement(new Statement[] {
                    entryIncStatement,
                    maybeBlockStatement
            }, currentVariableScope);
            rewritten.setSourcePosition(maybeBlockStatement);
            rewritten.setStatementLabel(maybeBlockStatement.getStatementLabel());
            return rewritten;
        } else {
            return maybeBlockStatement; // don't know how to handle it
        }
    }

    private Statement instrumentBlockStatement(BlockStatement blockStatement, Statement entryIncStatement) {
        final List<Statement> originalStatements = (List<Statement>) blockStatement.getStatements();
        final List<Statement> newStatements = newLinkedList();

        boolean isBlockPrefacedWithCtorCall = isCtorCallFirst(originalStatements);
        // non-constructor prefaced blocks can happily accept entry inc statements first
        if (entryIncStatement != null && !isBlockPrefacedWithCtorCall) {
            newStatements.add(entryIncStatement);
        }

        boolean firstStatementSeen = false;
        for (Statement statement : originalStatements) {
            // do not instrument 'break' statements; reason: we don't want to change return value of the case block
            // (which is calculated in a statement before - see CLOV-1341)
            final Statement instrStatement = (statement instanceof BreakStatement) ? null : instrumentStmt(statement);

            // add instrumentation *before* the statement if not the constructor call of a ctor block
            if (instrStatement != null && (!isBlockPrefacedWithCtorCall || firstStatementSeen)) {
                newStatements.add(instrStatement);
            }

            newStatements.add(statement);

            if (isBlockPrefacedWithCtorCall && !firstStatementSeen) {
                // first add the method entry
                if (entryIncStatement != null) {
                    newStatements.add(entryIncStatement);
                }

                // add instrumentation *after* the statement if ctor and the first statement (and not before 'break')
                if (instrStatement != null) {
                    newStatements.add(instrStatement);
                }
            }
            firstStatementSeen = true;
        }

        BlockStatement rewritten = new BlockStatement(newStatements, blockStatement.getVariableScope());
        rewritten.setSourcePosition(blockStatement);
        rewritten.setStatementLabel(blockStatement.getStatementLabel());

        return rewritten;
    }

    private boolean isCtorCallFirst(List<Statement> originalStatements) {
        if (originalStatements.size() > 0) {
            final Statement statement = originalStatements.get(0);
            if (statement instanceof ExpressionStatement) {
                final ExpressionStatement expressionStmt = (ExpressionStatement) statement;
                if (expressionStmt.getExpression() instanceof ConstructorCallExpression) {
                    return ((ConstructorCallExpression) expressionStmt.getExpression()).isSpecialCall();
                }
            }
        }
        return false;
    }

    private Statement instrumentStmt(Statement statement) {
        if (!isStatementInstrEnabled) {
            return null;
        }

        final FixedSourceRegion srcRegion = GroovyUtils.newRegionFor(statement);
        if (srcRegion == null) {
            return null;
        }

        int complexity = 0;
        if (statement instanceof SwitchStatement) {
            complexity = ((SwitchStatement)statement).getCaseStatements().size();
        } else if (statement instanceof TryCatchStatement) {
            complexity = ((TryCatchStatement)statement).getCatchStatements().size();
        }
        return Grover.recorderInc(
                classRef,
                session.addStatement(
                        new com.atlassian.clover.context.ContextSet(),
                        srcRegion, complexity, LanguageConstruct.Builtin.STATEMENT), statement);
    }
}
