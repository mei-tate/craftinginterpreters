package jlox.nativefunctions;

import jlox.Environment;
import jlox.NativeFunction;
import jlox.RuntimeError;

public class SystemNatives {

  public static void register(Environment environment) {

    // Unix timestamp in seconds.
    environment.define("time", new NativeFunction("time", 0,
        (interpreter, args) ->
            (double) System.currentTimeMillis() / 1000.0
    ));

    // Unix timestamp in milliseconds.
    environment.define("timeMillis", new NativeFunction("timeMillis", 0,
        (interpreter, args) ->
            (double) System.currentTimeMillis()
    ));

    // Pause execution for a specified number of seconds.
    environment.define("sleep", new NativeFunction("sleep", 1,
        (interpreter, args) -> {
          Object value = args.get(0);

          if (!(value instanceof Double)) {
            throw new RuntimeError(null,
                "sleep() expects a number.");
          }

          double seconds = (double) value;

          if (!Double.isFinite(seconds) || seconds < 0) {
            throw new RuntimeError(null,
                "sleep() expects a non-negative finite number.");
          }

          long millis = (long) (seconds * 1000);

          try {
            Thread.sleep(millis);
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeError(null,
                "sleep() was interrupted.");
          }

          return null;
        }
    ));

  }
}
