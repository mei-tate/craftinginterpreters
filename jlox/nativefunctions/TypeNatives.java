package jlox.nativefunctions;

import jlox.Environment;
import jlox.LoxCallable;
import jlox.NativeFunction;

public class TypeNatives {
  public static void register(Environment environment) {
    environment.define("type", new NativeFunction("type", 1,
        (interpreter, args) -> getType(args.get(0))
    ));
  }

  private static String getType(Object value) {
    if (value == null) return "nil";
    if (value instanceof Double) return "number";
    if (value instanceof String) return "string";
    if (value instanceof Boolean) return "boolean";
    if (value instanceof LoxCallable) return "function";
    return "object";
  }
}
