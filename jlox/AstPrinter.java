package jlox;

import java.util.ArrayList;
import java.util.List;

/**
 * AstPrinter
 *
 * <p>Helper class to visualize the AST of code you write. Call it in Lox.java instead of
 * interpreter.
 */
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  // print() overloads for expressions and statements
  String print(Expr expr) {
    return expr.accept(this);
  }

  String print(Stmt stmt) {
    return stmt.accept(this);
  }

  // Visit methods for each statement type
  @Override
  public String visitBlockStmt(Stmt.Block stmt) {
    StringBuilder builder = new StringBuilder("(block");

    for (Stmt statement : stmt.statements) {
      builder.append(" ");
      builder.append(statement.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitClassStmt(Stmt.Class stmt) {
    StringBuilder builder = new StringBuilder("(class ");
    builder.append(stmt.name.lexeme);

    for (Stmt.Function method : stmt.methods) {
      builder.append(" ");
      builder.append(method.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return parenthesize("expression", stmt.expression);
  }

  @Override
  public String visitFunctionStmt(Stmt.Function stmt) {
    StringBuilder builder = new StringBuilder("(fun ");
    builder.append(stmt.name.lexeme);
    builder.append(" (");

    for (int i = 0; i < stmt.params.size(); i++) {
      if (i > 0) builder.append(" ");
      builder.append(stmt.params.get(i).lexeme);
    }

    builder.append(")");

    for (Stmt statement : stmt.body) {
      builder.append(" ");
      builder.append(statement.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitIfStmt(Stmt.If stmt) {
    StringBuilder builder = new StringBuilder("(if ");
    builder.append(stmt.condition.accept(this));
    builder.append(" ");
    builder.append(stmt.thenBranch.accept(this));

    if (stmt.elseBranch != null) {
      builder.append(" ");
      builder.append(stmt.elseBranch.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
  }

  @Override
  public String visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null) {
      return "(return)";
    }

    return parenthesize("return", stmt.value);
  }

  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    if (stmt.initializer == null) {
      return "(var " + stmt.name.lexeme + ")";
    }

    return "(var " + stmt.name.lexeme + " " + stmt.initializer.accept(this) + ")";
  }

  @Override
  public String visitWhileStmt(Stmt.While stmt) {
    return "(while " + stmt.condition.accept(this) + " " + stmt.body.accept(this) + ")";
  }

  @Override
  public String visitBreakStmt(Stmt.Break stmt) {
    return "(break)";
  }

  // Visit methods for each expression type
  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize("=", new Expr.Variable(expr.name), expr.value);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitConditionalExpr(Expr.Conditional expr) {
    return parenthesize("?:", expr.condition, expr.thenBranch, expr.elseBranch);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return "("
        + "set "
        + expr.object.accept(this)
        + " "
        + expr.name.lexeme
        + " "
        + expr.value.accept(this)
        + ")";
  }

  @Override
  public String visitThisExpr(Expr.This expr) {
    return "this";
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  @Override
  public String visitCallExpr(Expr.Call expr) {
    List<Expr> expressions = new ArrayList<>();
    expressions.add(expr.callee);
    expressions.addAll(expr.arguments);

    return parenthesize("call", expressions.toArray(new Expr[0]));
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return "(" + "get " + expr.object.accept(this) + " " + expr.name.lexeme + ")";
  }

  @Override
  public String visitFunctionExpr(Expr.Function expr) {
    return parenthesize("fun");
  }

  /** Wraps a name and a list of subexpressions in parentheses. */
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);

    for (Expr expr : exprs) {
      builder.append(" ");
      builder.append(expr.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }
}
