package jlox;

import java.util.List;

/**
 * Represents an object that can be called, such as a function or class.
 *
 * <p>The call() method receives the interpreter executing the call and a list of evaluated argument
 * values. It returns the result of the call expression.
 */
interface LoxCallable {
  // Number of arguments a function or operation expects.
  int arity();

  Object call(Interpreter interpreter, List<Object> arguments);
}
