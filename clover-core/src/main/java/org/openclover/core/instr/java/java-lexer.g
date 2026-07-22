header {
    package org.openclover.core.instr.java;

    import org.openclover.core.cfg.instr.java.JavaInstrumentationConfig;
}

//----------------------------------------------------------------------------
// The Java scanner
//----------------------------------------------------------------------------
class JavaLexer extends Lexer;

options {
    exportVocab=JavaLexer;      // call the vocabulary "JavaLexer" (JavaLexerTokenTypes.txt)
    testLiterals=false;         // don't automatically test for literals
    k=4;                        // four characters of lookahead
    charVocabulary='\u0000'..'\uFFFE';
    // without inlining some bitset tests, couldn't do unicode;
    // I need to make ANTLR generate smaller bitsets; see
    // bottom of JavaLexer.java
    codeGenBitsetTestThreshold=30;
}

tokens {
    NUM_INT;
    CHAR_LITERAL;
    STRING_LITERAL;
    NUM_FLOAT;
    NUM_LONG;
    NUM_DOUBLE;

    // java reserved keywords
    ABSTRACT="abstract"; ASSERT="assert";
    BOOLEAN="boolean"; BREAK="break"; BYTE="byte";
    CASE="case"; CATCH="catch"; CHAR="char"; CLASS="class"; CONTINUE="continue";
    DEFAULT="default"; DO="do"; DOUBLE="double";
    ELSE="else"; ENUM="enum"; EXTENDS="extends";
    FINAL="final"; FINALLY="finally"; FLOAT="float"; FOR="for";
    IF="if"; IMPLEMENTS="implements"; IMPORT="import"; INSTANCEOF="instanceof"; INT="int"; INTERFACE="interface";
    LONG="long";
    NATIVE="native"; NEW="new";
    PACKAGE="package"; PRIVATE="private"; PROTECTED="protected"; PUBLIC="public";
    RETURN="return";
    SHORT="short"; STATIC="static"; STRICTFP="strictfp"; SUPER="super"; SWITCH="switch"; SYNCHRONIZED="synchronized";
    THIS="this"; THROW="throw"; THROWS="throws"; TRANSIENT="transient"; TRY="try";
    VOID="void"; VOLATILE="volatile";
    WHILE="while";

    // reserved keywords, but unused
    CONST="const"; GOTO="goto";

    // reserved literals
    TRUE="true"; FALSE="false"; NULL="null";
}

{
    private boolean nonComment = false;
    private int lineCount = 1; // ##HACK - to cope with 1 line files that have no new line
    private int ncLineCount = 0;
    private JavaInstrumentationConfig mConfig;

    public JavaLexer(Reader in, JavaInstrumentationConfig aCfg) {
        this(new CharBuffer(in));
        setTabSize(1);
        mConfig = aCfg;
    }
    
	protected void nc() {
		nonComment = true;
	}

	public void newline() {
		lineCount++;
		if (nonComment) {
			ncLineCount++;
			nonComment = false;
		}
		super.newline();
	}

	public int getLineCount() {
		return lineCount;
	}

	public int getNCLineCount() {
		return ncLineCount;
	}
}

// we treat "non-sealed" as a special token; this is not defined on the list of tokens, with the literal,
// because definition of MINUS '-' would kick-in before detection of "non-sealed", thus it would never yield
// a NON_SEALED token, but the "IDENT MINUS IDENT" instead
// due to the 4-character lookahead, the rule is not perfect and will match for any "non-" prefix
// we can't increase lookahead due to performance reasons
NON_SEALED
    :
        "non" MINUS "sealed"
        {nc();}
    ;

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
        // don't match a newline always, because there might not be one!
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
    // the opening delimiter of a text block may be followed by some (unbounded) amount of whitespace
    // before the mandatory line terminator, so a fixed lookahead cannot tell it apart from an empty/short
    // single-line string literal - a syntactic predicate (with backtracking) is used instead
    : ( '"' '"' '"' (' '|'\t'|'\f')* ('\r'|'\n') ) => STRING_LITERAL_TEXT_BLOCK
    | STRING_LITERAL_SINGLE_LINE
    ;

protected STRING_LITERAL_SINGLE_LINE
    : {nc();}   '"' ( ESC | ~( '"' | '\\' | '\n' | '\r') )* '"'
    ;

protected STRING_LITERAL_TEXT_BLOCK
    : {nc();} '"' '"' '"' (' '|'\t'|'\f')*
        (   ( '\r' '\n' ) => '\r' '\n' {newline();}
          | '\r'                       {newline();}
          | '\n'                       {newline();}
        )
        (   ( (BACKSLASH)? '"' '"' ~'"' ) => (BACKSLASH)? '"' '"'
          | ( (BACKSLASH)? '"'     ~'"' ) => (BACKSLASH)? '"'
          | ( '\r' '\n' ) => '\r' '\n' {newline();}
          | '\r'                       {newline();}
          | '\n'                       {newline();}
          | (ESC) => ESC
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
 * literals table to see if it's a literal or really an identifier
 */
IDENT
options { testLiterals=true; }
    :
        { nc(); } IdentifierStart (IdentifierPart)*
    ;

// a complete rewrite of the overly "spaghettified" NUM_INT rule, to support hex floats
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



// ------------------------------------------------------------------------------------------------
// Identifier characters.
//
// These two rules are deliberately coarse *superset* of the real Java identifier characters:
// '$'/'_', the relevant ASCII letters (plus digits for the part rule), and the whole non-ASCII BMP.
// That is all ANTLR needs to route a token to IDENT and to consume its characters, and because a
// large character set is emitted as a single bitset match/test (see codeGenBitsetTestThreshold above),
// the generated code is compact and O(1) per character (no giant switch over hundreds of ranges).
//
// IdentifierStart excludes the low-surrogate range (a lone low surrogate cannot start an identifier);
// high surrogates are included, and the trailing low surrogate of a supplementary-plane identifier is
// then consumed by the (IdentifierPart)* closure, which does include the low-surrogate range.
protected IdentifierStart
    :   '\u0024'              // '$'
    |   '\u0041'..'\u005a'    // 'A'..'Z'
    |   '\u005f'              // '_'
    |   '\u0061'..'\u007a'    // 'a'..'z'
    |   '\u0080'..'\udbff'    // non-ASCII BMP up to and including the high-surrogate range
    |   '\ue000'..'\ufffe'    // non-ASCII BMP after the low-surrogate range
    ;

protected IdentifierPart
    :   '\u0024'              // '$'
    |   '\u0030'..'\u0039'    // '0'..'9'
    |   '\u0041'..'\u005a'    // 'A'..'Z'
    |   '\u005f'              // '_'
    |   '\u0061'..'\u007a'    // 'a'..'z'
    |   '\u0080'..'\ufffe'    // whole non-ASCII BMP (incl. surrogates, so surrogate pairs are consumed)
    ;
