package org.openclover.groovy.instr;

import org.openclover.core.api.registry.SourceInfo;
import org.openclover.core.registry.FixedSourceRegion;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.AttributeExpression;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.BooleanExpression;
import org.codehaus.groovy.ast.expr.CastExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.ast.expr.ClosureListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.FieldExpression;
import org.codehaus.groovy.ast.expr.GStringExpression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.MapEntryExpression;
import org.codehaus.groovy.ast.expr.MapExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.MethodPointerExpression;
import org.codehaus.groovy.ast.expr.NotExpression;
import org.codehaus.groovy.ast.expr.PostfixExpression;
import org.codehaus.groovy.ast.expr.PrefixExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.RangeExpression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.codehaus.groovy.ast.expr.SpreadMapExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.expr.TernaryExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.UnaryMinusExpression;
import org.codehaus.groovy.ast.expr.UnaryPlusExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionRegionTracker extends CodeVisitorSupport {
    private int line = Integer.MAX_VALUE;
    private int col = Integer.MAX_VALUE;
    private int lastLine = Integer.MIN_VALUE;
    private int lastCol = Integer.MIN_VALUE;

    private void maybeGrowRegion(@NotNull Expression expression) {
        line = expression.getLineNumber() == -1 ? line : Math.min(line, expression.getLineNumber());
        col = expression.getColumnNumber() == -1 ? col : Math.min(col, expression.getColumnNumber());
        lastLine = expression.getLastLineNumber() == -1 ? lastLine : Math.max(lastLine, expression.getLastLineNumber());
        lastCol = expression.getLastColumnNumber() == -1 ? lastCol : Math.max(lastCol, expression.getLastColumnNumber());
    }

    @Nullable
    public SourceInfo getRegion() {
        return (line >= 1 && lastLine >= line && col >= 1 &&
                ((lastLine == line && lastCol >= col) || lastLine > line)) ? // 'lastCol >= col' only for single line
                new FixedSourceRegion(line, col, lastLine, lastCol) : null;
    }

    @Override
    public void visitBytecodeExpression(BytecodeExpression expression) {
        maybeGrowRegion(expression);
        super.visitBytecodeExpression(expression);
    }

    @Override
    public void visitMethodCallExpression(MethodCallExpression expression) {
        maybeGrowRegion(expression);
        super.visitMethodCallExpression(expression);
    }

    @Override
    public void visitStaticMethodCallExpression(StaticMethodCallExpression expression) {
        maybeGrowRegion(expression);
        super.visitStaticMethodCallExpression(expression);
    }

    @Override
    public void visitConstructorCallExpression(ConstructorCallExpression expression) {
        maybeGrowRegion(expression);
        super.visitConstructorCallExpression(expression);
    }

    @Override
    public void visitBinaryExpression(BinaryExpression expression) {
        maybeGrowRegion(expression);
        super.visitBinaryExpression(expression);
    }

    @Override
    public void visitTernaryExpression(TernaryExpression expression) {
        maybeGrowRegion(expression);
        super.visitTernaryExpression(expression);
    }

    @Override
    public void visitShortTernaryExpression(ElvisOperatorExpression expression) {
        maybeGrowRegion(expression);
        super.visitShortTernaryExpression(expression);
    }

    @Override
    public void visitPostfixExpression(PostfixExpression expression) {
        maybeGrowRegion(expression);
        super.visitPostfixExpression(expression);
    }

    @Override
    public void visitPrefixExpression(PrefixExpression expression) {
        maybeGrowRegion(expression);
        super.visitPrefixExpression(expression);
    }

    @Override
    public void visitBooleanExpression(BooleanExpression expression) {
        maybeGrowRegion(expression);
        super.visitBooleanExpression(expression);
    }

    @Override
    public void visitNotExpression(NotExpression expression) {
        maybeGrowRegion(expression);
        super.visitNotExpression(expression);
    }

    @Override
    public void visitClosureExpression(ClosureExpression expression) {
        maybeGrowRegion(expression);
        super.visitClosureExpression(expression);
    }

    @Override
    public void visitTupleExpression(TupleExpression expression) {
        maybeGrowRegion(expression);
        super.visitTupleExpression(expression);
    }

    @Override
    public void visitListExpression(ListExpression expression) {
        maybeGrowRegion(expression);
        super.visitListExpression(expression);
    }

    @Override
    public void visitArrayExpression(ArrayExpression expression) {
        maybeGrowRegion(expression);
        super.visitArrayExpression(expression);
    }

    @Override
    public void visitMapExpression(MapExpression expression) {
        maybeGrowRegion(expression);
        super.visitMapExpression(expression);
    }

    @Override
    public void visitMapEntryExpression(MapEntryExpression expression) {
        maybeGrowRegion(expression);
        super.visitMapEntryExpression(expression);
    }

    @Override
    public void visitRangeExpression(RangeExpression expression) {
        maybeGrowRegion(expression);
        super.visitRangeExpression(expression);
    }

    @Override
    public void visitSpreadExpression(SpreadExpression expression) {
        maybeGrowRegion(expression);
        super.visitSpreadExpression(expression);
    }

    @Override
    public void visitSpreadMapExpression(SpreadMapExpression expression) {
        maybeGrowRegion(expression);
        super.visitSpreadMapExpression(expression);
    }

    @Override
    public void visitMethodPointerExpression(MethodPointerExpression expression) {
        maybeGrowRegion(expression);
        super.visitMethodPointerExpression(expression);
    }

    @Override
    public void visitUnaryMinusExpression(UnaryMinusExpression expression) {
        maybeGrowRegion(expression);
        super.visitUnaryMinusExpression(expression);
    }

    @Override
    public void visitUnaryPlusExpression(UnaryPlusExpression expression) {
        maybeGrowRegion(expression);
        super.visitUnaryPlusExpression(expression);
    }

    @Override
    public void visitBitwiseNegationExpression(BitwiseNegationExpression expression) {
        maybeGrowRegion(expression);
        super.visitBitwiseNegationExpression(expression);
    }

    @Override
    public void visitCastExpression(CastExpression expression) {
        maybeGrowRegion(expression);
        super.visitCastExpression(expression);
    }

    @Override
    public void visitConstantExpression(ConstantExpression expression) {
        maybeGrowRegion(expression);
        super.visitConstantExpression(expression);
    }

    @Override
    public void visitClassExpression(ClassExpression expression) {
        maybeGrowRegion(expression);
        super.visitClassExpression(expression);
    }

    @Override
    public void visitVariableExpression(VariableExpression expression) {
        maybeGrowRegion(expression);
        super.visitVariableExpression(expression);
    }

    @Override
    public void visitDeclarationExpression(DeclarationExpression expression) {
        maybeGrowRegion(expression);
        super.visitDeclarationExpression(expression);
    }

    @Override
    public void visitPropertyExpression(PropertyExpression expression) {
        maybeGrowRegion(expression);
        super.visitPropertyExpression(expression);
    }

    @Override
    public void visitAttributeExpression(AttributeExpression expression) {
        maybeGrowRegion(expression);
        super.visitAttributeExpression(expression);
    }

    @Override
    public void visitFieldExpression(FieldExpression expression) {
        maybeGrowRegion(expression);
        super.visitFieldExpression(expression);
    }

    @Override
    public void visitGStringExpression(GStringExpression expression) {
        maybeGrowRegion(expression);
        super.visitGStringExpression(expression);
    }

    @Override
    public void visitArgumentlistExpression(ArgumentListExpression expression) {
        maybeGrowRegion(expression);
        super.visitArgumentlistExpression(expression);
    }

    @Override
    public void visitClosureListExpression(ClosureListExpression expression) {
        maybeGrowRegion(expression);
        super.visitClosureListExpression(expression);
    }
}
