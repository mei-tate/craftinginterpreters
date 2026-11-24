package jlox;

import java.util.List;

/**
 * 
 * NativeFunction
*/
public class NativeFunction implements LoxCallable {
  private final String name;
  private final int arity;
  private final NativeBody body;

  public NativeFunction(String name, int arity, NativeBody body) {
    this.name = name;
    this.arity = arity;
    this.body = body;
  }

  @Override
  public int arity() {
    return arity;
  }

  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    return body.call(interpreter, arguments);
  }

  @Override
  public String toString() {
    return "<native fn " + name + ">";
  }

  @FunctionalInterface
  public interface NativeBody {
    Object call(Interpreter interpreter, List<Object> arguments);
  }
}