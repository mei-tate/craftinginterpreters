package jlox.nativefunctions;

import jlox.Environment;
import jlox.NativeFunction;
import jlox.RuntimeError;

public class StringNatives {

  public static void register(Environment environment) {

    environment.define("len", new NativeFunction("len", 1,
        (interpreter, args) ->
            (double) string(args.get(0), "len").length()
    ));

    environment.define("lower", new NativeFunction("lower", 1,
        (interpreter, args) ->
            string(args.get(0), "lower").toLowerCase()
    ));

    environment.define("upper", new NativeFunction("upper", 1,
        (interpreter, args) ->
            string(args.get(0), "upper").toUpperCase()
    ));

    environment.define("trim", new NativeFunction("trim", 1,
        (interpreter, args) ->
            string(args.get(0), "trim").trim()
    ));

    environment.define("contains", new NativeFunction("contains", 2,
        (interpreter, args) ->
            string(args.get(0), "contains")
                .contains(string(args.get(1), "contains"))
    ));

    environment.define("startsWith", new NativeFunction("startsWith", 2,
        (interpreter, args) ->
            string(args.get(0), "startsWith")
                .startsWith(string(args.get(1), "startsWith"))
    ));

    environment.define("endsWith", new NativeFunction("endsWith", 2,
        (interpreter, args) ->
            string(args.get(0), "endsWith")
                .endsWith(string(args.get(1), "endsWith"))
    ));

    environment.define("replace", new NativeFunction("replace", 3,
        (interpreter, args) ->
            string(args.get(0), "replace")
                .replace(
                    string(args.get(1), "replace"),
                    string(args.get(2), "replace")
                )
    ));

    environment.define("substring", new NativeFunction("substring", 3,
        (interpreter, args) -> {
          String value = string(args.get(0), "substring");
          int start = index(args.get(1), "substring");
          int end = index(args.get(2), "substring");

          if (start < 0 || end < start || end > value.length()) {
            throw new RuntimeError(null,
                "substring() indices are out of bounds.");
          }

          return value.substring(start, end);
        }
    ));

  }

  private static String string(Object value, String function) {
    if (value instanceof String) {
      return (String) value;
    }

    throw new RuntimeError(null,
        function + "() expects a string.");
  }

  private static int index(Object value, String function) {
    if (!(value instanceof Double)) {
      throw new RuntimeError(null,
          function + "() indices must be numbers.");
    }

    double number = (double) value;

    if (!Double.isFinite(number) || number != Math.floor(number)
        || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
      throw new RuntimeError(null,
          function + "() indices must be valid integers.");
    }

    return (int) number;
  }
}
