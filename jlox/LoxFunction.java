package jlox;

import java.util.List;

/**
 * Implements LoxCallable so that runtime behavior does not bleed into the front end's syntax
 * classes.
 */
class LoxFunction implements LoxCallable {
  private final Token name;
  private final List<Token> params;
  private final List<Stmt> body;
  private final Environment closure;
  private final boolean isInitializer;

  LoxFunction(
      Token name, List<Token> params, List<Stmt> body, Environment closure, boolean isInitializer) {
    this.isInitializer = isInitializer;
    this.name = name;
    this.params = params;
    this.body = body;
    this.closure = closure;
  }

  LoxFunction bind(LoxInstance instance) {
    Environment environment = new Environment(closure);
    // The resolver assigns `this` slot 0 in the method's enclosing scope, so it
    // must live in the slot-backed locals rather than the globals map.
    environment.defineAt(0, instance);
    return new LoxFunction(name, params, body, environment, isInitializer);
  }

  @Override
  public String toString() {
    return "<fn " + (name == null ? "anonymous" : name.lexeme) + ">";
  }

  @Override
  public int arity() {
    return params.size();
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    // Creates a new environment at each function call.
    Environment environment = new Environment(closure);

    for (int i = 0; i < params.size(); i++) {
      environment.defineAt(i, arguments.get(i));
    }

    try {
      // Restore the previous environment after executing the function.
      interpreter.executeBlock(body, environment);
    } catch (Return returnValue) {
      if (isInitializer) return closure.getAt(0, 0);

      return returnValue.value;
    }

    if (isInitializer) return closure.getAt(0, 0);
    return null;
  }
}
