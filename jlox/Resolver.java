package jlox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Local>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private int loopDepth = 0;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  private enum FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }

  private enum ClassType {
    NONE,
    CLASS
  }

  private ClassType currentClass = ClassType.NONE;

  private static class Local {
    final int slot;
    final Token name;
    boolean defined;
    boolean used;

    Local(int slot, Token name) {
      this.slot = slot;
      this.name = name;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    declare(stmt.name);
    define(stmt.name);

    beginScope();
    Token thisToken = new Token(TokenType.THIS, "this", null, -1);
    Local thisLocal = new Local(0, thisToken);
    thisLocal.defined = true;
    scopes.peek().put("this", thisLocal);

    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method, declaration);
    }

    endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) resolve(stmt.initializer);
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty()) {
      Local local = scopes.peek().get(expr.name.lexeme);
      if (local != null && !local.defined)
        Lox.error(expr.name, "Can't read local variable in its own initializer.");
    }
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);
    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE)
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword, "Can't return a value from an initializer.");
      }

      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    loopDepth++;
    try {
      resolve(stmt.body);
    } finally {
      loopDepth--;
    }
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);
    for (Expr arg : expr.arguments) resolve(arg);
    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword, "Can't use 'this' outside of a class.");
      return null;
    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitFunctionExpr(Expr.Function expr) {
    resolveFunction(expr, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitConditionalExpr(Expr.Conditional expr) {
    resolve(expr.condition);
    resolve(expr.thenBranch);
    resolve(expr.elseBranch);
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    if (loopDepth == 0) Lox.error(stmt.keyword, "Cannot use 'break' outside a loop.");
    return null;
  }

  // Below are Helpers
  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  void resolve(List<Stmt> statements) {
    boolean returned = false;
    for (Stmt statement : statements) {
      if (statement == null) continue;
      if (returned) Lox.warning(statementToken(statement), "Unreachable code after return.");
      resolve(statement);
      if (statement instanceof Stmt.Return) returned = true;
    }
  }

  private Token statementToken(Stmt stmt) {
    if (stmt instanceof Stmt.Var) return ((Stmt.Var) stmt).name;
    if (stmt instanceof Stmt.Function) return ((Stmt.Function) stmt).name;
    if (stmt instanceof Stmt.Return) return ((Stmt.Return) stmt).keyword;
    if (stmt instanceof Stmt.Break) return ((Stmt.Break) stmt).keyword;
    if (stmt instanceof Stmt.Expression)
      return ((Stmt.Expression) stmt).expression instanceof Expr.Variable
          ? ((Expr.Variable) ((Stmt.Expression) stmt).expression).name
          : null;
    if (stmt instanceof Stmt.Print)
      return ((Stmt.Print) stmt).expression instanceof Expr.Variable
          ? ((Expr.Variable) ((Stmt.Print) stmt).expression).name
          : null;
    if (stmt instanceof Stmt.Block && !((Stmt.Block) stmt).statements.isEmpty())
      return statementToken(((Stmt.Block) stmt).statements.get(0));
    if (stmt instanceof Stmt.While) return statementToken(((Stmt.While) stmt).body);
    if (stmt instanceof Stmt.If) return statementToken(((Stmt.If) stmt).thenBranch);
    return null;
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    int enclosingLoopDepth = loopDepth;
    currentFunction = type;
    loopDepth = 0;
    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
    loopDepth = enclosingLoopDepth;
  }

  private void resolveFunction(Expr.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    int enclosingLoopDepth = loopDepth;
    currentFunction = type;
    loopDepth = 0;
    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
    loopDepth = enclosingLoopDepth;
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Local>());
  }

  private void endScope() {
    Map<String, Local> scope = scopes.pop();
    for (Local local : scope.values()) {
      if (!local.used)
        Lox.warning(local.name, "Local variable '" + local.name.lexeme + "' is never used.");
    }
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;
    Map<String, Local> scope = scopes.peek();
    if (scope.containsKey(name.lexeme))
      Lox.error(name, "Already a variable with this name in this scope.");
    Local local = new Local(scope.size(), name);
    scope.put(name.lexeme, local);
    interpreter.resolveDeclaration(name, local.slot);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().get(name.lexeme).defined = true;
  }

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      Local local = scopes.get(i).get(name.lexeme);
      if (local != null) {
        local.used = true;
        interpreter.resolve(expr, scopes.size() - 1 - i, local.slot);
        return;
      }
    }
  }
}
