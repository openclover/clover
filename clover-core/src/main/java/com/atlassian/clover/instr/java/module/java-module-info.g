
header {
package com.atlassian.clover.instr.java.module;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

import com.atlassian.clover.cfg.instr.java.JavaInstrumentationConfig;
import com.atlassian.clover.instr.java.CloverToken;
import com.atlassian.clover.instr.java.CloverTokenStreamFilter;
import com.atlassian.clover.instr.java.ContextTreeNode;
import com.atlassian.clover.instr.java.FileStructureInfo;
import com.atlassian.clover.instr.java.TokenListUtil;
import com.atlassian.clover.registry.*;
import com.atlassian.clover.registry.entities.*;

}


class JavaModuleInfoRecognizer extends Parser;
options {
    defaultErrorHandler = false;     // Don't generate parser error handlers
    k = 2;                           // two token lookahead
    exportVocab=JavaModuleInfo;        // Call its vocabulary "JavaModuleInfo"
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

    /**
     * Counts the number of LT seen in the typeArguments production.
     * It is used in semantic predicates to ensure we have seen
     * enough closing '>' characters; which actually may have been
     * either GT, SR or BSR tokens.
     */
    private int ltCounter = 0;

    public JavaModuleInfoRecognizer(CloverTokenStreamFilter filter, JavaInstrumentationConfig cfg, FileStructureInfo fileInfo, ContextTreeNode contextTreeRoot) {
        this(filter);
    }

}

moduleDeclaration
{
    AnnotationImpl an;
    String moduleName;
}
    :
        ( an=annotation )*
        ( "open" )?
        "module" moduleName=identifier
        LCURLY
        ( moduleDirective )*
        RCURLY
    ;

moduleDirective
    :
        (
            requiresDirective
        |
            exportsDirective
        |
            opensDirective
        |
            usesDirective
        |
            providesDirective
        )
    ;

requiresDirective
{
    String requiredModule;
}
    :
        "requires"
        ( "transitive" | "static" )?
        requiredModule=identifier
        SEMI!
    ;

exportsDirective
{
    String exportedPackage;
    String moduleName;
}
    :
        "exports"
        exportedPackage=identifier
        (
            "to"
            moduleName=identifier ( COMMA! moduleName=identifier )*
        )?
        SEMI!
    ;

opensDirective
{
    String openedPackage;
    String moduleName;
}
    :
        "opens"
        openedPackage=identifier
        (
            "to"
             moduleName=identifier ( COMMA! moduleName=identifier )*
        )?
        SEMI!
    ;


usesDirective
{
    String serviceName;
}
    :
        "uses"
        serviceName=identifier
        SEMI!
    ;


providesDirective
{
    String serviceName;
    String withType;
}
    :
        "provides"
        serviceName=identifier
        "with"
        withType=identifier
        (
            COMMA!
            withType=identifier
        )?
        SEMI!
    ;


// Compilation Unit: In Java, this is a single file.  This is the start rule for this parser
compilationUnit
    :
        // Next we have a series of zero or more import statements
        ( importDefinition )*

        // JLS specifies two kinds of compilation unit: ordinary and modular
        // here we have module declaration
        moduleDeclaration

        EOF!
    ;

// Import statement: import followed by a package or class name
//    or a "static" method import
importDefinition
    options {defaultErrorHandler = false;}
    :   "import" ("static")? identifierStar SEMI!
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


declaratorBrackets returns [String brackets]
{
    brackets = "";
}
    :   (LBRACK RBRACK! {brackets += "[]";})*
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


// A list of formal parameters
parameterDeclarationList returns [Parameter [] params]
{
  List parameters = new ArrayList();
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
    assignmentExpression
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
    :
        logicalOrExpression
        (
            QUESTION assignmentExpression COLON! conditionalExpression
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
    |   unaryExpressionNotPlusMinus
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
 *    unaryExpressionNotPlusMinus ->
 *    LPAREN classTypeSpec RPAREN unaryExpressionNotPlusMinus ->
 *    postfixExpression
 * </pre>
 */
unaryExpressionNotPlusMinus
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
            (LPAREN type=classTypeSpec (BAND type=classOrInterfaceType)* RPAREN unaryExpressionNotPlusMinus)=>
            LPAREN  type=classTypeSpec (BAND type=classOrInterfaceType)* RPAREN
            unaryExpressionNotPlusMinus
        |
            postfixExpression
        )
    ;

/**
 * Qualified names, array expressions, method invocation, post inc/dec
 *
 * @param classCastStart - used for instrumentation of a method reference with type cast
 * @param classCastEnd - used for instrumentation of a method reference with type cast
 */
postfixExpression
    :
        primaryExpressionPart         // start with a primary part like constant or identifier
        supplementaryExpressionPart   // add extra stuff like array indexes, this/super (optional)
        (
            (methodReferencePredicate) =>
            methodReferencePart           // add a method reference or
        |
            supplementaryPostIncDecPart   // possibly add on a post-increment or post-decrement.
        )
    ;



// the basic element of an expression
primaryExpressionPart
{
  String type = null;
}    :   IDENT
    |   constant
    |   "true"
    |   "false"
    |   "this"
    |   "null"
    |   LPAREN! assignmentExpression RPAREN!
    |   "super"
        // look for int.class and int[].class
    |   type=builtInType
        ( LBRACK  RBRACK! )*
        DOT "class"
    ;

/**
 * A supplementary part for the primaryExpressionPart, which allows us to use array indexes, dot-qualified names,
 * this/class/super calls etc. Shall be used in conjunction with the primaryExpressionPart.
 */
supplementaryExpressionPart
    :
        (
            // qualified id (id.id.id.id...) -- build the name
            DOT (
                  (typeArguments)? // a prefix to a generic class to supply type arguments
                  IDENT
                | "this"
                | "class"
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


argList
    :   (   expressionList
        |   /*nothing*/
        )
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
       anno = annotation2
    ;

protected
annotation2 returns [AnnotationImpl anno]
{
  boolean ft = false;
  CloverToken ident = null; 
  boolean hasArgs = false;
  String annotationName = null;
  anno = new AnnotationImpl();
}
    :
        AT annotationName=identifier { ident=(CloverToken)LT(0);anno.setName(annotationName);}
        (
            lparen:LPAREN RPAREN // normalAnnotationRest
        |    (LPAREN IDENT ASSIGN )=> LPAREN annMemberValuePair[anno] (COMMA annMemberValuePair[anno])* RPAREN {hasArgs=true;} // normalAnnotation
        |    LPAREN ft=annMemberValue2[anno, "value", true] RPAREN {hasArgs=true;} // singleMemberAnnotation
           // none means just a markerAnnotation
        )?
    ;

protected
annMemberValuePair [AnnotationImpl anno]
{
  boolean ft = false;
}
    :
        name:IDENT ASSIGN ft = annMemberValue2[anno, name.getText(), true]
    ;


protected
annMemberValue2 [AnnotationValueCollection anno, String key, boolean makeArrayAndAdd] returns [boolean wasFallthroughLiteral]
{
  AnnotationImpl innerAnno = null;
  wasFallthroughLiteral = false;
  String expr = null;
}
    :
        (STRING_LITERAL) => t:STRING_LITERAL
        {   wasFallthroughLiteral = "\"fallthrough\"".equalsIgnoreCase(t.getText());

            if (anno != null) anno.put(key, new StringifiedAnnotationValue(t.getText()));
        }

        | expr = conditionalExpression2 {if (anno != null) anno.put(key, new StringifiedAnnotationValue(expr));}
        | innerAnno=annotation {if (anno != null) anno.put(key, innerAnno);}
        | annMemberValueArrayInitializer [anno, key]
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
annMemberValueArrayInitializer [AnnotationValueCollection anno, String key]
{
  boolean seenFallthrough = false;
  ArrayAnnotationValue annoArray = (anno == null) ? null : new ArrayAnnotationValue();
  if (anno != null) {
  	anno.put(key, annoArray);
  }
}
    :
        LCURLY
        (seenFallthrough = annMemberValues[annoArray])?
        (COMMA)?
        RCURLY
    ;

protected
annMemberValues [ArrayAnnotationValue annoArray] returns [boolean sawFallthrough]
{
  sawFallthrough = false;
  boolean tmp;
}
    :
    	//null use as annotation array values are not named
        tmp=annMemberValue2[annoArray, null, false]
        {
            if (tmp) {
              sawFallthrough = true;
            }
        }
        (
	    	//null used as annotation array values are not named
            COMMA tmp=annMemberValue2[annoArray, null, false]
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
class JavaModuleInfoLexer extends Lexer;

options {
    exportVocab=JavaModuleInfo;      // call the vocabulary "JavaModuleInfo"
    testLiterals=false;    // don't automatically test for literals
    k=4;                   // four characters of lookahead
    charVocabulary='\u0000'..'\uFFFE';
    // without inlining some bitset tests, couldn't do unicode;
    // I need to make ANTLR generate smaller bitsets; see
    // bottom of JavaModuleInfoLexer.java
    codeGenBitsetTestThreshold=30;
}

{
    private boolean noncomment = false;
    private int linecount = 1; // ##HACK - to cope with 1 line files that have no new line
    private int ncLinecount = 0;
    private JavaInstrumentationConfig mConfig;
    private boolean isModuleInfoDeclaration = false;

    public JavaModuleInfoLexer(Reader in, JavaInstrumentationConfig aCfg) {
        this(new CharBuffer(in));
        setTabSize(1);
        mConfig = aCfg;
    }

    public int testLiteralsTable(int aType)
    {
        int tmpType = super.testLiteralsTable(aType);
        return tmpType;
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
 */
STRING_LITERAL
    : {nc();}   '"' ( ESC | ~( '"' | '\\' | '\n' | '\r') )* '"'
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
 * Thus, the JavaModuleInfoLexer shall be able to handle both forms.
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
