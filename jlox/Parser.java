package jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Parser {
  private static class ParseError extends RuntimeException {}

  // Consume a flat input sequence of tokens.
  private final List<Token> tokens;
  // Use 'current' to point to the next token to be parsed.
  private int current = 0;
  private int loopDepth = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  /**
   * program → declaration* EOF ; Parses a series of declarations, as many as it can find until it
   * hits the end of the input.
   */
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }

  List<Stmt> parsePrompt() {
    if (check(TokenType.FUN)
        || check(TokenType.VAR)
        || check(TokenType.FOR)
        || check(TokenType.IF)
        || check(TokenType.RETURN)
        || check(TokenType.PRINT)
        || check(TokenType.WHILE)
        || check(TokenType.BREAK)
        || check(TokenType.LEFT_BRACE)) return parse();
    try {
      Expr expr = expression();
      match(TokenType.SEMICOLON);
      if (!isAtEnd()) throw error(peek(), "Expect end of input.");
      return java.util.Collections.singletonList(new Stmt.Expression(expr));
    } catch (ParseError error) {
      synchronize();
      return new ArrayList<>();
    }
  }

  /** declaration → classDecl | funDecl | varDecl | statement ; */
  private Stmt declaration() {
    try {
      if (match(TokenType.CLASS)) return classDeclaration();
      if (match(TokenType.FUN)) return function("function");
      // Check  if we’re at a variable declaration
      if (match(TokenType.VAR)) return varDeclaration();
      // If not, it falls through to the existing statement() method that parses print and
      // expression statements.
      return statement();
    } catch (ParseError error) {
      synchronize();
      return null;
    }
  }

  /** classDecl → "class" IDENTIFIER ( "<" IDENTIFIER )? "{" function* "}" ; */
  private Stmt.Class classDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
    consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

    List<Stmt.Function> methods = new ArrayList<>();
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
    return new Stmt.Class(name, methods);
  }

  /**
   * funDecl → "fun" function ; function → IDENTIFIER "(" parameters? ")" block ; parameters →
   * IDENTIFIER ( "," IDENTIFIER )* ;
   */
  private Stmt.Function function(String kind) {
    Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
    consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");

    List<Token> parameters = new ArrayList<>();
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
      } while (match(TokenType.COMMA));
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");

    // block() assumes the brace token has already been matched.
    // Consuming it here lets us report a more precise error message if the { isn’t found.
    consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }

  /** varDecl → "var" IDENTIFIER ( "=" expression )? ";" ; */
  private Stmt varDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
    Expr initializer = null;
    if (match(TokenType.EQUAL)) {
      initializer = expression();
    }

    consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
    return new Stmt.Var(name, initializer);
  }

  /**
   * statement → exprStmt | forStmt | ifStmt | returnStmt | printStmt | whileStmt | breakStmt |
   * block ;
   */
  private Stmt statement() {
    if (match(TokenType.FOR)) return forStatement();
    if (match(TokenType.IF)) return ifStatement();
    if (match(TokenType.RETURN)) return returnStatement();
    if (match(TokenType.PRINT)) return printStatement();
    if (match(TokenType.WHILE)) return whileStatement();
    if (match(TokenType.BREAK)) return breakStatement();
    if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }

  /**
   * exprStmt → expression ";" ; Parse an expression followed by a semicolon. We wrap that Expr in a
   * Stmt of the right type and return it.
   */
  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }

  /**
   * forStmt → "for" "(" ( varDecl | exprStmt | ";" ) expression? ";" expression? ")" statement ;
   */
  private Stmt forStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    // If the token following the ( is a semicolon then the initializer has been omitted.
    if (match(TokenType.SEMICOLON)) {
      initializer = null;
      // Otherwise, we check for a var keyword to see if it’s a variable declaration.
    } else if (match(TokenType.VAR)) {
      initializer = varDeclaration();
      // If neither of those matched, it must be an expression.
    } else {
      initializer = expressionStatement();
    }

    // Look for a semicolon to see if the clause has been omitted.
    Expr condition = null;
    if (!check(TokenType.SEMICOLON)) {
      condition = expression();
    }
    consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

    // Increment
    Expr increment = null;
    if (!check(TokenType.RIGHT_PAREN)) {
      increment = expression();
    }
    consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

    Stmt body = statement();
    // Increment, if there is one, executes after the body in each iteration of the loop. We do that
    // by replacing the body with a little block that contains the original body followed by an
    // expression statement that evaluates the increment.
    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
    }

    // Take the condition and the body and build the loop using a primitive while loop.
    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

    // If there is an initializer, it runs once before the entire loop.
    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  /** ifStmt → "if" "(" expression ")" statement ( "else" statement )? ; */
  private Stmt ifStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    // The else is bound to the nearest if that precedes it.
    if (match(TokenType.ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  /** returnStmt → "return" expression? ";" ; */
  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    // Since a semicolon can’t begin an expression, if the next token is, we know there must not be
    // a return value.
    if (!check(TokenType.SEMICOLON)) {
      value = expression();
    }

    consume(TokenType.SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }

  /**
   * printStmt → "print" expression ";" ; Parse the expression, consume the terminating semicolon,
   * and emit the syntax tree.
   */
  private Stmt printStatement() {
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }

  /** returnStmt → "return" expression? ";" ; */

  /** whileStmt → "while" "(" expression ")" statement ; */
  private Stmt whileStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

    loopDepth++;
    Stmt body;
    try {
      body = statement();
    } finally {
      loopDepth--;
    }

    return new Stmt.While(condition, body);
  }

  /** break → "break" ";" ; */
  private Stmt breakStatement() {
    Token keyword = previous();
    consume(TokenType.SEMICOLON, "Expect ';' after 'break'.");

    if (loopDepth == 0) {
      error(keyword, "Can't use 'break' outside of a loop.");
    }

    return new Stmt.Break(keyword);
  }

  /** block → "{" declaration* "}" ; */
  private List<Stmt> block() {
    // Create an empty list
    List<Stmt> statements = new ArrayList<>();

    // Parse statements and add them to the list until we reach the end of the block, marked by the
    // closing }.
    // To avoid infinite loops we also check for isAtEnd(), in case user forgets a closing }.
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
    return statements;
  }

  /** expression → comma ; */
  private Expr expression() {
    return comma();
  }

  /** comma → assignment ( "," assignment )* ; */
  private Expr comma() {
    Expr expr = assignment();

    // the ( ... )* loop in the rule maps to a while loop.
    while (match(TokenType.COMMA)) {
      Token operator = previous();
      Expr right = assignment();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /** assignment → ( call "." )? IDENTIFIER "=" assignment | logic_or ; */
  private Expr assignment() {
    Expr expr = conditional();

    if (match(TokenType.EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, value);
        // Handle turning an Expr.Get expression on the left into the corresponding Expr.Set.
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get) expr;
        return new Expr.Set(get.object, get.name, value);
      }

      error(equals, "Invalid assignment target.");
    }

    return expr;
  }

  /** conditional → logic_or ( "?" expression ":" conditional )? ; */
  private Expr conditional() {
    Expr expr = or();

    if (match(TokenType.QUESTION)) {
      Expr thenBranch = expression();
      consume(TokenType.COLON, "Expect ':' after then branch.");
      Expr elseBranch = conditional();
      expr = new Expr.Conditional(expr, thenBranch, elseBranch);
    }

    return expr;
  }

  /** logic_or → logic_and ( "or" logic_and )* ; */
  private Expr or() {
    Expr expr = and();

    while (match(TokenType.OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  /** logic_and → equality ( "and" equality )* ; */
  private Expr and() {
    Expr expr = equality();

    while (match(TokenType.AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  /** equality → comparison ( ( "!=" | "==" ) comparison )* ; */
  private Expr equality() {
    if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous();
      comparison();
      throw error(operator, "Missing left-hand operand.");
    }

    Expr expr = comparison();

    // the ( ... )* loop in the rule maps to a while loop.
    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /**
   * Checks to see if the current token has any of the given types. If so, it comsumes the token and
   * returns true. Otherwise, it returns false and leaves the current token alone.
   */
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the current token is of the given type. Never consumes the token, only checks
   * it.
   */
  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  /** Consumes the current token and returns it */
  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  /** Checks if we’ve run out of tokens to parse. */
  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }

  /** Returns the current token we have yet to consume */
  private Token peek() {
    return tokens.get(current);
  }

  /** Returns the most recently consumed token. */
  private Token previous() {
    return tokens.get(current - 1);
  }

  /** comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )* ; */
  private Expr comparison() {
    if (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = previous();
      term();
      throw error(operator, "Missing left-hand operand.");
    }

    Expr expr = term();

    while (match(
        TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /** term → factor ( ( "-" | "+" ) factor )* ; */
  private Expr term() {
    if (match(TokenType.PLUS)) {
      Token operator = previous();
      factor();
      throw error(operator, "Missing left-hand operand.");
    }

    Expr expr = factor();

    while (match(TokenType.MINUS, TokenType.PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /** factor → unary ( ( "/" | "*" ) unary )* ; */
  private Expr factor() {
    if (match(TokenType.SLASH, TokenType.STAR)) {
      Token operator = previous();
      unary();
      throw error(operator, "Missing left-hand operand.");
    }

    Expr expr = unary();

    while (match(TokenType.SLASH, TokenType.STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  /** unary → ( "!" | "-" ) unary | primary ; */
  private Expr unary() {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

    return call();
  }

  /** call → primary ( "(" arguments? ")" | "." IDENTIFIER )* ; */
  private Expr call() {
    // Parse a primary expression, the “left operand” to the call.
    Expr expr = primary();

    while (true) {
      // Each time we see a (, we call finishCall() to parse the call expression using the
      // previously parsed expression as the callee.
      if (match(TokenType.LEFT_PAREN)) {
        expr = finishCall(expr);
      } else if (match(TokenType.DOT)) {
        Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
      } else {
        break;
      }
    }

    return expr;
  }

  /** arguments → expression ( "," expression )* ; Also handles the zero-argument case. */
  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    // Parse an expression, then look for a comma indicating that there is another argument after
    // that.
    // When we don’t find a comma, then the argument list must be done and we consume the expected
    // closing parenthesis.
    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        // Cap amount of arguments you can pass to a function to no more than 255.
        if (arguments.size() >= 255) {
          error(peek(), "Can't have more than 255 arguments.");
        }
        arguments.add(assignment());
      } while (match(TokenType.COMMA));
    }
    // If zero-argument case, don’t try to parse any arguments.
    Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }

  /**
   * primary → "true" | "false" | "nil" | "this" | NUMBER | STRING | IDENTIFIER | "(" expression ")"
   * | "super" "." IDENTIFIER ; | lambda
   */
  private Expr primary() {
    if (match(TokenType.FALSE)) return new Expr.Literal(false);
    if (match(TokenType.TRUE)) return new Expr.Literal(true);
    if (match(TokenType.NIL)) return new Expr.Literal(null);

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(TokenType.THIS)) return new Expr.This(previous());

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    if (match(TokenType.FUN)) {
      return lambda();
    }
    throw error(peek(), "Expect expression.");
  }

  /** lambda → "fun" "(" parameters? ")" block ; */
  private Expr lambda() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'fun'.");

    List<Token> parameters = new ArrayList<>();

    if (!check(TokenType.RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 255) {
          error(peek(), "Can't have more than 255 parameters.");
        }

        parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
      } while (match(TokenType.COMMA));
    }

    consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
    consume(TokenType.LEFT_BRACE, "Expect '{' before function body.");
    List<Stmt> body = block();

    return new Expr.Function(parameters, body);
  }

  /**
   * Checks to see if the next token is of the expected type. If so, it consumes the token and
   * everything is good. If some other token is there, then we’ve hit an error.
   */
  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }

  /** Reports an error at a given token. Shows the token’s location and the token itself. */
  private ParseError error(Token token, String message) {
    Lox.error(token, message);
    return new ParseError();
  }

  /**
   * Discards tokens until it thinks it has found a statement boundary. After catching a ParseError
   * this is called to get back in sync.
   */
  private void synchronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON) return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }
}
