package jlox.nativefunctions;

import java.util.Scanner;
import jlox.Environment;
import jlox.NativeFunction;
import jlox.RuntimeError;

public class InputOutputNatives {
  private static final Scanner INPUT = new Scanner(System.in);

  public static void register(Environment environment) {
    environment.define("input", new NativeFunction("input", 0,
        (interpreter, args) -> {
          if (!INPUT.hasNextLine()) {
            throw new RuntimeError(null, "input() has no more lines to read.");
          }
          return INPUT.nextLine();
        }
    ));
  }
}
