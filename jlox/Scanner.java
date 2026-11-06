package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("and", TokenType.AND);
    keywords.put("class", TokenType.CLASS);
    keywords.put("else", TokenType.ELSE);
    keywords.put("false", TokenType.FALSE);
    keywords.put("for", TokenType.FOR);
    keywords.put("fun", TokenType.FUN);
    keywords.put("if", TokenType.IF);
    keywords.put("nil", TokenType.NIL);
    keywords.put("or", TokenType.OR);
    keywords.put("print", TokenType.PRINT);
    keywords.put("return", TokenType.RETURN);
    keywords.put("super", TokenType.SUPER);
    keywords.put("this", TokenType.THIS);
    keywords.put("true", TokenType.TRUE);
    keywords.put("var", TokenType.VAR);
    keywords.put("while", TokenType.WHILE);
    keywords.put("break", TokenType.BREAK);
  }

  Scanner(String source) {
    this.source = source;
  }

  /**
   * Scanner works its way through the source code, adding tokens until it runs out of characters.
   * Then it appends one final “end of file” token.
   */
  List<Token> scanTokens() {
    while (!isAtEnd()) {
      // We are at the beginning of the next lexme.
      start = current;
      scanToken();
    }

    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(TokenType.LEFT_PAREN);
        break;
      case ')':
        addToken(TokenType.RIGHT_PAREN);
        break;
      case '{':
        addToken(TokenType.LEFT_BRACE);
        break;
      case '}':
        addToken(TokenType.RIGHT_BRACE);
        break;
      case ',':
        addToken(TokenType.COMMA);
        break;
      case '.':
        addToken(TokenType.DOT);
        break;
      case '-':
        addToken(TokenType.MINUS);
        break;
      case '+':
        addToken(TokenType.PLUS);
        break;
      case ';':
        addToken(TokenType.SEMICOLON);
        break;
      case ':':
        addToken(TokenType.COLON);
        break;
      case '?':
        addToken(TokenType.QUESTION);
        break;
      case '*':
        addToken(TokenType.STAR);
        break;
      case '!':
        addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
        break;
      case '=':
        addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
        break;
      case '<':
        addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
        break;
      case '>':
        addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
        break;
      case '/':
        if (match('/')) {
          // Single-line comment: consume until the end of the line.
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) {
          // Block comment: consume until */
          blockComment();
        } else {
          addToken(TokenType.SLASH);
        }
        break;

      case ' ':
      case '\r':
      case '\t':
        // Ignore whitespace.
        break;

      case '\n':
        line++;
        break;

      case '"':
        string();
        break;

      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();

        } else {
          Lox.error(line, "Unexpected character.");
        }
        break;
    }
  }

  /** Checks for C-style block comments. */
  private void blockComment() {
    while (!isAtEnd()) {
      // Check for closing '*/'.
      if (peek() == '*' && peekNext() == '/') {
        advance(); // Consume '*'
        advance(); // Consume '/'
        return;
      }
      if (peek() == '\n') {
        // track line numbers for error reporting
        line++;
      }
      advance();
    }
    // If loop finishes, the block comment was never closed
    Lox.error(line, "Unterminated block comment.");
  }

  /**
   * When we scan an identifier check to see if it matches anything in the map. If so, use that
   * keyword’s token type. Otherwise, it’s a regular user-defined identifier.
   */
  private void identifier() {
    while (isAlphaNumeric(peek())) advance();
    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null) type = TokenType.IDENTIFIER;
    addToken(type);
  }

  /**
   * Consume as many digits as we find for the integer part of the literal. Then we look for a
   * fractional part, which is a decimal point (.) followed by at least one digit. If there is a
   * fractional part, again consume as many digits as we can find.
   */
  private void number() {
    while (isDigit(peek())) advance();

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peekNext())) {
      // Consume the "."
      advance();
      while (isDigit(peek())) advance();
    }
    //  Uses Java’s own parsing method to convert the lexeme to a real Java double.
    addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  /**
   * Consume characters until we hit the " that ends the string. Reports error if we run out of
   * input before the string is closed.
   */
  private void string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    String value = source.substring(start + 1, current - 1);
    addToken(TokenType.STRING, value);
  }

  /**
   * Conditional advance(). We only consume the current character if it's what we're looking for.
   */
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  /** Looks ahead one character (doesn't consume the character). */
  private char peek() {
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }

  /**
   * Checks if a character is a digit. Note: The Java standard library provides Character.isDigit(),
   * which seems like a good fit. But, that method allows things like Devanagari digits, full-width
   * numbers, and other stuff we don’t want.
   */
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  /** Checks if character is an alphabet character. */
  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  /** Helper function that tells us if we've consumed all the characters. */
  private boolean isAtEnd() {
    return current >= source.length();
  }

  /** Helper for scanToken input, consumes the next character in the source file and returns it. */
  private char advance() {
    return source.charAt(current++);
  }

  /**
   * Helper for scanToken output, grabs the text of the current lexeme and creates a new token for
   * it. Adds a token without a literal value.
   */
  private void addToken(TokenType type) {
    addToken(type, null);
  }

  /** Adds a token with an optional literal value. */
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
