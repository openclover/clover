header {

package com.atlassian.clover.reporters;

import com.atlassian.clover.api.registry.BlockMetrics;

}

class CalcParser extends Parser;
options {
	buildAST = true;	// uses CommonAST by default
	defaultErrorHandler=false;
}

expr
	:	factor ((PLUS^|MINUS^) factor)*
	;

factor
	:	atom ((STAR^|SLASH^|POW^) atom)*
	;
	

atom:	INT
    |   (FMT)? CLOVDATA
    |   LPAREN! expr RPAREN!
	;


class CalcLexer extends Lexer;

WS	:	(' '
	|	'\t'
	|	'\n'
	|	'\r')
		{ _ttype = Token.SKIP; }
	;

LPAREN:	'('
	;

RPAREN:	')'
	;

STAR:	'*'
	;

SLASH:	'/'
	;

POW:	'^'
	;

PLUS:	'+'
	;

MINUS:	'-'
	;

SEMI:	';'
	;

protected
DIGIT
	:	'0'..'9'
	;

INT	:	(DIGIT)+
	;

FMT :   '%'
    ;

CLOVDATA
    : ('a'..'z'|'A'..'Z')+
    ;

class CalcTreeWalker extends TreeParser;
options {
	defaultErrorHandler=false;
}

expr [BlockMetrics m] returns [double r]
{
	double a,b;
	r=0;
}
	:
		#(PLUS  a=expr[m] b=expr[m])	{r = a+b;}
	|	#(MINUS a=expr[m] b=expr[m])	{r = a-b;}
	|	#(STAR  a=expr[m] b=expr[m])    {r = a*b;}
	|	#(SLASH a=expr[m] b=expr[m])	{r = a/b;}
	|	#(POW   a=expr[m] b=expr[m])    {r = Math.pow(a, b);}
	|	i:INT			                {r = (float)Integer.parseInt(i.getText());}
	|   (f:FMT)? c:CLOVDATA
            {
                String fmt = f == null ? "raw" : f.getText();
                try {
                    r = Columns.getColumnValue(c.getText(), fmt, m);
                    if ("%".equals(fmt)) {
                        //None of our percentage columns should ever be negative
                        //but sometimes their raw value might be (-100)
                        //where the denominator was 0 - TODO: fix this hack by using NaN throughout
                        r = Math.max(0d, r);
                    }

                } catch (com.atlassian.clover.api.CloverException e) {
                    throw new SemanticException(e.getMessage());
                }
            }	
	;

validate
	:
		#(PLUS  validate validate)
	|	#(MINUS validate validate)
	|	#(STAR  validate validate)
	|	#(SLASH validate validate)
	|	#(POW   validate validate)
	|	i:INT
	|   (f:FMT)? c:CLOVDATA
            {
                if (!Columns.isValidColumnName(c.getText())) {
                    throw new SemanticException("Invalid column name: '" + c.getText() + "'");
                }
            }
	;
