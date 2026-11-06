package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interpreter: declares that it’s a visitor. The return type of the visit methods will be Object,
 * the root class that we use to refer to a Lox value in our Java code.
 */
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  // globals holds a fixed reference to the outermost global environment.
  final Environment globals = new Environment();
  // Tracks the current environment.
  private Environment environment = globals;

  // Stores resolution information
  private static class LocalAddress {
    final int depth, slot;

    LocalAddress(int depth, int slot) {
      this.depth = depth;
      this.slot = slot;
    }
  }

  private final Map<Expr, LocalAddress> locals = new HashMap<>();
  private final Map<Token, Integer> declarationSlots = new HashMap<>();

  Interpreter() {
    // Defines a variable named “clock”.
    globals.define(
        "clock",
        new LoxCallable() {
          @Override
          // The clock() function takes no arguments (its arity is zero).
          public int arity() {
            return 0;
          }

          @Override
          // Calls the corresponding Java function and converts the result to a double value in
          // seconds.
          public Object call(Interpreter interpreter, List<Object> arguments) {
            return (double) System.currentTimeMillis() / 1000.0;
          }

          @Override
          public String toString() {
            return "<native fn>";
          }
        });
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  void interpretPrompt(List<Stmt> statements) {
    if (statements.size() == 1 && statements.get(0) instanceof Stmt.Expression) {
      try {
        Object value = evaluate(((Stmt.Expression) statements.get(0)).expression);
        System.out.println(stringify(value));
      } catch (RuntimeError error) {
        Lox.runtimeError(error);
      }
    } else {
      interpret(statements);
    }
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth, int slot) {
    locals.put(expr, new LocalAddress(depth, slot));
  }

  void resolveDeclaration(Token name, int slot) {
    declarationSlots.put(name, slot);
  }

  /** Evaluating Block Statements */
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  /** Evaluating Class Statements */
  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    // Declare the class's name in the current environment.
    environment.define(stmt.name.lexeme, null);

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
      LoxFunction function =
          new LoxFunction(
              method.name,
              method.params,
              method.body,
              environment,
              method.name.lexeme.equals("init"));
      methods.put(method.name.lexeme, function);
    }

    LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
    // Store the class object in the variable we previously declared.
    environment.assign(stmt.name, klass);
    return null;
  }

  /** Executes a list of statements in the context of a given environment. */
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;

      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  /** Evaluating Expression Statements */
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  /** Evaluating Functions */
  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt.name, stmt.params, stmt.body, environment, false);

    Integer slot = declarationSlots.get(stmt.name);
    if (slot == null) environment.define(stmt.name.lexeme, function);
    else environment.defineAt(slot, function);
    return null;
  }

  /** Evaluating If-Then-Else Statements */
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    // Uses Java's If-ElseIf-Else feature
    if (isTruth(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  /** Evaluating Print Statements */
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  /** Evaluating Return Statements */
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);

    throw new Return(value);
  }

  /** Evaluating declaration statements. */
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    // If the variable has an initializer, we evaluate it.
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    // Else Lox sets a variable to nil if it isn’t explicitly initialized.
    Integer slot = declarationSlots.get(stmt.name);
    if (slot == null) environment.define(stmt.name.lexeme, value);
    else environment.defineAt(slot, value);
    return null;
  }

  /** Evaluating While Loops. */
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    // Uses Java's while feature.
    try {
      while (isTruth(evaluate(stmt.condition))) {
        execute(stmt.body);
      }
    } catch (Break breakLoop) {
      // Exit the nearest enclosing loop.
    }

    return null;
  }

  /** Evaluating Breaks. */
  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new Break();
  }

  private static class Break extends RuntimeException {
    Break() {
      super(null, null, false, false);
    }
  }

  /** Evaluating Assignments. */
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);

    // Look up the variable’s scope distance. If not found, we assume it’s global and handle it the
    // same way as before. Otherwise call assignAt().
    LocalAddress address = locals.get(expr);
    if (address != null) {
      environment.assignAt(address.depth, address.slot, value);
    } else {
      globals.assign(expr.name, value);
    }

    return value;
  }

  /**
   * Evaluating literals We produced the runtime value during scanning and stuffed it in the token.
   * So to evaluate a literal, we simply pull it back out.
   */
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  /** Evaluating Logical Expressions AND, OR */
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    // Associativity: Left-to-right
    Object left = evaluate(expr.left);

    if (expr.operator.type == TokenType.OR) {
      if (isTruth(left)) return left;
    } else {
      if (!isTruth(left)) return left;
    }

    return evaluate(expr.right);
  }

  /** Evaluating Class's Set Expressions */
  @Override
  public Object visitSetExpr(Expr.Set expr) {
    // Evaluate the object whose property is being set and check to see if it’s a LoxInstance. If
    // not, that’s a runtime error.
    Object object = evaluate(expr.object);
    if (!(object instanceof LoxInstance)) {
      throw new RuntimeError(expr.name, "Only instances have fields.");
    }

    // Evaluate the value being set and store it on the instance.
    Object value = evaluate(expr.value);
    ((LoxInstance) object).set(expr.name, value);
    return value;
  }

  /** Evaluating Class's This Expressions */
  @Override
  public Object visitThisExpr(Expr.This expr) {
    return lookUpVariable(expr.keyword, expr);
  }

  /**
   * Evaluating parentheses To evaluate the grouping expression itself, we recursively evaluate that
   * subexpression and return it.
   */
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  /** Sends the expression back into the interpreter’s visitor implementation. */
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  /** Evaluating Conditionals */
  @Override
  public Object visitConditionalExpr(Expr.Conditional expr) {
    Object condition = evaluate(expr.condition);

    if (isTruth(condition)) {
      return evaluate(expr.thenBranch);
    }

    return evaluate(expr.elseBranch);
  }

  /**
   * Evaluating unary expressions First, we evaluate the operand expression. Then we apply the unary
   * operator itself to the result of that.
   */
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case TokenType.BANG:
        return !isTruth(right);
      case TokenType.MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double) right;
    }

    // Unreachable.
    return null;
  }

  /** Evaluating variable expressions. */
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  /**
   * Look up the resolved distance in the map. If we don’t find a distance in the map, it must be
   * global. In that case, look it up dynamically, directly in the global environment.
   */
  private Object lookUpVariable(Token name, Expr expr) {
    LocalAddress address = locals.get(expr);
    if (address != null) {
      return environment.getAt(address.depth, address.slot);
    } else {
      return globals.get(name);
    }
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  /** Runtime Error Reporting */
  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;

    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  /** Lox follows Ruby’s rule: false and nil are false, and everything else is truth. */
  private boolean isTruth(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean) object;
    return true;
  }

  /** Evaluating binary operators. */
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case TokenType.BANG_EQUAL:
        return !isEqual(left, right);
      case TokenType.EQUAL_EQUAL:
        return isEqual(left, right);
      case TokenType.GREATER:
        checkNumberOperands(expr.operator, left, right);
        checkNumberOperand(expr.operator, right);
        return (double) left > (double) right;
      case TokenType.GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left >= (double) right;
      case TokenType.LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left < (double) right;
      case TokenType.LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double) left <= (double) right;
      case TokenType.MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double) left - (double) right;
      case TokenType.PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double) left + (double) right;
        }
        // The + operator can also be used to concatenate two strings.
        if (left instanceof String && right instanceof String) {
          return (String) left + (String) right;
        }
        // If either operand is a string, the other is converted to a string and the results are
        // then concatenated.
        // For example, "scone" + 4 would yield scone4
        if ((left instanceof String && right instanceof Double)
            || (left instanceof Double && right instanceof String)) {
          return left.toString() + right.toString();
        }
        throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
      case TokenType.SLASH:
        checkNumberOperands(expr.operator, left, right);
        if ((double) right == 0.0) {
          throw new RuntimeError(expr.operator, "Cannot divide by zero.");
        }
        return (double) left / (double) right;
      case TokenType.STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double) left * (double) right;
    }

    // Unreachable.
    return null;
  }

  /** Evaluating Functions. */
  @Override
  public Object visitCallExpr(Expr.Call expr) {
    // Evaluate the expression for the callee
    Object callee = evaluate(expr.callee);

    // Evaluate each of the argument expressions in order and store the resulting values in a list.
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    // Strings aren’t callable in Lox.
    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    // Cast the callee to a LoxCallable and invoke a call.
    LoxCallable function = (LoxCallable) callee;
    // Check the number of arguments a function or operation expects.
    // Raises a runtime error if the argument list is too short or too long.
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(
          expr.paren,
          "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  /** Evaluating Class's Get Expressions */
  @Override
  public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);
    if (object instanceof LoxInstance) {
      return ((LoxInstance) object).get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
  }

  /** Evaluating Lambdas. */
  @Override
  public Object visitFunctionExpr(Expr.Function expr) {
    return new LoxFunction(null, expr.params, expr.body, environment, false);
  }

  /**
   * Checks equality logic. The equality operators support operands of any type, even mixed ones.
   */
  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;

    return a.equals(b);
  }

  /** Converts value to string */
  private String stringify(Object object) {
    if (object == null) return "nil";

    // Lox uses double-precision numbers even for integer values. In that case, they should print
    // without a decimal point. Since Java has both floating point and integer types, it wants you
    // to know which one you’re using. It tells you by adding an explicit .0 to integer-valued
    // doubles. We don’t care about that, so we hack it off the end.
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }

    return object.toString();
  }
}
