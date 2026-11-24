package jlox.nativefunctions;

import jlox.Environment;
import jlox.NativeFunction;
import jlox.RuntimeError;

public class MathNatives {

  public static void register(Environment environment) {

    environment.define("min", new NativeFunction("min", 2,
        (interpreter, args) ->
            Math.min(number(args.get(0), "min"),
                     number(args.get(1), "min"))
    ));

    environment.define("max", new NativeFunction("max", 2,
        (interpreter, args) ->
            Math.max(number(args.get(0), "max"),
                     number(args.get(1), "max"))
    ));

    environment.define("floor", new NativeFunction("floor", 1,
        (interpreter, args) ->
            Math.floor(number(args.get(0), "floor"))
    ));

    environment.define("ceil", new NativeFunction("ceil", 1,
        (interpreter, args) ->
            Math.ceil(number(args.get(0), "ceil"))
    ));

    environment.define("round", new NativeFunction("round", 1,
        (interpreter, args) ->
            (double) Math.round(number(args.get(0), "round"))
    ));

    environment.define("abs", new NativeFunction("abs", 1,
        (interpreter, args) ->
            Math.abs(number(args.get(0), "abs"))
    ));

    environment.define("sqrt", new NativeFunction("sqrt", 1,
        (interpreter, args) ->
            Math.sqrt(number(args.get(0), "sqrt"))
    ));

    environment.define("pow", new NativeFunction("pow", 2,
        (interpreter, args) ->
            Math.pow(number(args.get(0), "pow"),
                     number(args.get(1), "pow"))
    ));

    environment.define("random", new NativeFunction("random", 0,
        (interpreter, args) -> Math.random()
    ));

    environment.define("randomInt", new NativeFunction("randomInt", 2,
        (interpreter, args) -> {
          int min = integer(args.get(0), "randomInt");
          int max = integer(args.get(1), "randomInt");

          if (min > max) {
            throw new RuntimeError(null,
                "randomInt() minimum cannot exceed maximum.");
          }

          return (double) java.util.concurrent.ThreadLocalRandom
              .current().nextLong(min, (long) max + 1);
        }
    ));
  }

  private static double number(Object value, String function) {
    if (value instanceof Double) {
      return (double) value;
    }

    throw new RuntimeError(null,
        function + "() expects a number.");
  }

  private static int integer(Object value, String function) {
    double number = number(value, function);
    if (!Double.isFinite(number) || number != Math.floor(number)
        || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
      throw new RuntimeError(null,
          function + "() expects integer bounds.");
    }
    return (int) number;
  }
}
