package parser;
import java.io.*;   
import javax.swing.text.Segment;   
import java.io.Reader;
import org.fife.ui.rsyntaxtextarea.*;  
%%   

%public   
%class BDDTokenMaker   
%extends AbstractJFlexCTokenMaker   
%unicode   
%type org.fife.ui.rsyntaxtextarea.Token   

/**   
 * A simple TokenMaker example.   
 */   
%{   

   /**   
    * Constructor.  This must be here because JFlex does not generate a   
    * no-parameter constructor.   
    */   
   public BDDTokenMaker() {   
   }   


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @param link Whether this token is a hyperlink.
	 */
	private void addToken(int tokenType, boolean link) {
		int so = zzStartRead + offsetShift;
		super.addToken(zzBuffer, zzStartRead,zzMarkedPos-1, tokenType, so, link);
		zzStartRead = zzMarkedPos;
	}

	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @param link Whether this token is a hyperlink.
	 */
	private void addToken(int tokenType) {
		addToken(tokenType,false);
	}

	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addHyperlinkToken(int, int, int)
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}

	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addToken(int, int, int)
	 */
	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}

	/**
	 * Always returns <code>Token.NULL</code>, as there are no multiline
	 * tokens in properties files.
	 *
	 * @param text The line of tokens to examine.
	 * @param initialTokenType The token type to start with (i.e., the value
	 *        of <code>getLastTokenTypeOnLine</code> for the line before
	 *        <code>text</code>).
	 * @return <code>Token.NULL</code>.
	 */
	public int getLastTokenTypeOnLine(Segment text, int initialTokenType) {
		return Token.NULL;
	}

	/**
	 * Returns the text to place at the beginning and end of a
	 * line to "comment" it in a this programming language.
	 *
	 * @return <code>null</code>, as there are no comments in plain text.
	 */
	@Override
	public String[] getLineCommentStartAndEnd(int languageIndex) {
		return null;
	}

	/**
	 * Always returns <tt>false</tt>, as you never want "mark occurrences"
	 * working in plain text files.
	 *
	 * @param type The token type.
	 * @return Whether tokens of this type should have "mark occurrences"
	 *         enabled.
	 */
	public boolean getMarkOccurrencesOfTokenType(int type) {
		return false;
	}


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		s = text;
		try {
			yyreset(zzReader);
			yybegin(YYINITIAL);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new TokenImpl();
		}

	}    

	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 * @exception   IOException  if any I/O-Error occurs.
	 */
	private boolean zzRefill() {
		return zzCurrentPos>=s.offset+s.count;
	}

	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public final void yyreset(Reader reader) {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


%}

/* This part is taken from http://www.python.org/doc/2.2.3/ref/grammar.txt */
identifier			= (({letter}|"_")({letter}|{digit}|"_")*)
letter				= ({lowercase}|{uppercase})
lowercase			= ([a-z])
uppercase			= ([A-Z])
digit				= ([0-9])
stringliteral		= ({stringprefix}?{shortstring})
stringprefix		= ("r"|"u"[rR]?|"R"|"U"[rR]?)
shortstring1		= ([\']{shortstring1item}*[\']?)
shortstring2		= ([\"]{shortstring2item}*[\"]?)
shortstring			= ({shortstring1}|{shortstring2})
shortstring1item	= ({shortstring1char}|{escapeseq})
shortstring2item	= ({shortstring2char}|{escapeseq})
shortstring1char	= ([^\\\n\'])
shortstring2char	= ([^\\\n\"])
escapeseq			= ([\\].)
longinteger			= ({integer}[lL])
integer				= ({decimalinteger}|{octinteger}|{hexinteger})
decimalinteger		= ({nonzerodigit}{digit}*|"0")
octinteger			= ("0"{octdigit}+)
hexinteger			= ("0"[xX]{hexdigit}+)
nonzerodigit		= ([1-9])
octdigit			= ([0-7])
hexdigit			= ({digit}|[a-f]|[A-F])
floatnumber			= ({pointfloat}|{exponentfloat})
pointfloat			= ({intpart}?{fraction}|{intpart}".")
exponentfloat		= (({intpart}|{pointfloat}){exponent})
intpart				= ({digit}+)
fraction			= ("."{digit}+)
exponent			= ([eE][\+\-]?{digit}+)
imagnumber			= (({floatnumber}|{intpart})[jJ])

ErrorNumberFormat	= ({digit}{NonSeparator}+)
NonSeparator		= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#")

LongStringStart1	= ({stringprefix}?\'\'\')
LongStringStart2	= ({stringprefix}?\"\"\")

LineTerminator		= (\n)
WhiteSpace			= ([ \t\f])
LineComment			= ("#".*)


LetterOrDigit       = ([a-zA-Z0-9])
URLGenDelim		  	= ([:\/\?#\[\]@])
URLSubDelim		  	= ([\!\$&'\(\)\*\+,;=])
URLUnreserved		= ({LetterOrDigit}|[_\-\.\~])
URLCharacter		= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters		= ({URLCharacter}*)
URLEndCharacter		= ([\/\$]|{LetterOrDigit})
URL					= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


Data                = ("<"{identifier}">")
Tag                 = ("@"{identifier})

Feature             = ("Feature")
FeatureName         = {Feature}": "(.*{LineTerminator}?)
Elements	    = ("Background"|"Scenario"|"Scenario Outline"|"Examples")
Steps		    = ("Given"|"When"|"And"|"Then"|"But"){WhiteSpace}
Desc                = (" "[^"Feature"]*\n)
PipeVar 			= "#"("|"{identifier}{WhiteSpace}+"|")

%state LONG_STRING_1
%state LONG_STRING_2


%%


<YYINITIAL> {FeatureName}			{ 
                                    int len="Feature".length(); 
                                    addHyperlinkToken(zzStartRead,zzStartRead+len-1,Token.FUNCTION);
									addToken(zzStartRead+len,zzStartRead+len,Token.OPERATOR);
						 			addToken(zzStartRead+len+1,zzMarkedPos - 1,Token.LITERAL_BACKQUOTE);
									//out.println("Feature end:"+zzMarkedPos);
									}

<YYINITIAL> {Elements}":"			{ 
									addHyperlinkToken(zzStartRead,zzMarkedPos - 2,Token.RESERVED_WORD);
 									addToken(zzMarkedPos - 1,zzMarkedPos - 1,Token.OPERATOR);
									 }
								 
<YYINITIAL> {Steps}					{ addToken(Token.RESERVED_WORD); }
<YYINITIAL> {Data}					{ addToken(Token.DATA_TYPE); }
<YYINITIAL> {Tag}					{ addToken(Token.ANNOTATION); }
<YYINITIAL> {Desc}					{ addToken(Token.ANNOTATION); }
<YYINITIAL> {PipeVar}				{ 
									//addToken(zzStartRead,zzStartRead,Token.OPERATOR);
									//addToken(zzStartRead+1,zzMarkedPos - 2,Token.DATA_TYPE); 
									//addToken(zzMarkedPos -1,zzMarkedPos -1,Token.OPERATOR);
									}

/*url */
{URL}								{ addToken(Token.IDENTIFIER, true); }

<YYINITIAL> {

	{LineTerminator}				{ addNullToken(); return firstToken; }

	{identifier}					{ addToken(Token.IDENTIFIER); }

	{WhiteSpace}+					{ addToken(Token.WHITESPACE); }

	/* String/Character Literals. */
	{stringliteral}                 { addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	{LongStringStart1}				{ yybegin(LONG_STRING_1); addToken(Token.LITERAL_CHAR); }
	{LongStringStart2}				{ yybegin(LONG_STRING_2); addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }

	/* Comment Literals. */
	{LineComment}					{ addToken(Token.COMMENT_EOL); }

	/* Separators. */
	"("|")"|"["|"]"|"{"|"}"|"<"|">"			{ addToken(Token.SEPARATOR); }
	

	/* Operators. */
	"="|"+"|"-"|"*"|"/"|"%"|"**"			{ addToken(Token.OPERATOR); }
	"~"|"<"|">"|"<<"|">>"|"=="|"+="			{ addToken(Token.OPERATOR); }
	"-="|"*="|"/="|"%="|">>="|"<<="			{ addToken(Token.OPERATOR); }
	"^"|"&"|"&&"|"|"|"||"|"?"|":"|","		{ addToken(Token.OPERATOR); }
	"!"|"++"|"--"|"."|","					{ addToken(Token.OPERATOR); }

	/* Numbers */
	{longinteger}|{integer}			{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{floatnumber}|{imagnumber}		{ addToken(Token.LITERAL_NUMBER_FLOAT); }
	{ErrorNumberFormat}				{ addToken(Token.ERROR_NUMBER_FORMAT); }

	/* Other punctuation, we'll highlight it as "identifiers." */
	"@"							{ addToken(Token.IDENTIFIER); }
	";"							{ addToken(Token.IDENTIFIER); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters and flag them as bad. */
	.							{ addToken(Token.ERROR_IDENTIFIER); }

}



<LONG_STRING_1> {
	[^\']+						{ addToken(Token.LITERAL_CHAR); }
	"'''"						{ yybegin(YYINITIAL); addToken(Token.LITERAL_CHAR); }
	"'"							{ addToken(Token.LITERAL_CHAR); }
	<<EOF>>						{
                                	if (firstToken==null) {
										addToken(Token.LITERAL_CHAR); 
                                    }
                                    return firstToken;
								}
}

<LONG_STRING_2> {
	[^\"]+						{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	\"\"\"						{ yybegin(YYINITIAL); addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	\"							{ addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); }
	<<EOF>>						{
                                	if (firstToken==null) {
                                    	addToken(Token.LITERAL_STRING_DOUBLE_QUOTE); 
                                    }
                                    return firstToken;
								}
}
