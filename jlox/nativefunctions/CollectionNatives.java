package jlox.nativefunctions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jlox.Environment;
import jlox.NativeFunction;
import jlox.RuntimeError;

public class CollectionNatives {

  public static void register(Environment environment) {

    // Create an empty list.
    environment.define("list", new NativeFunction("list", 0,
        (interpreter, args) -> new ArrayList<Object>()
    ));

    // Return the number of elements in a list or map.
    environment.define("size", new NativeFunction("size", 1,
        (interpreter, args) -> {
          Object value = args.get(0);

          if (value instanceof List<?>) {
            return (double) ((List<?>) value).size();
          }

          if (value instanceof Map<?, ?>) {
            return (double) ((Map<?, ?>) value).size();
          }

          throw new RuntimeError(null,
              "size() expects a list or map.");
        }
    ));

    // Add an element to the end of a list.
    environment.define("push", new NativeFunction("push", 2,
        (interpreter, args) -> {
          List<Object> list = requireList(args.get(0), "push");
          list.add(args.get(1));
          return null;
        }
    ));

    // Remove and return the last element.
    environment.define("pop", new NativeFunction("pop", 1,
        (interpreter, args) -> {
          List<Object> list = requireList(args.get(0), "pop");

          if (list.isEmpty()) {
            throw new RuntimeError(null,
                "pop() cannot remove an element from an empty list.");
          }

          return list.remove(list.size() - 1);
        }
    ));

    // Reverse a list in place.
    environment.define("reverse", new NativeFunction("reverse", 1,
        (interpreter, args) -> {
          List<Object> list = requireList(args.get(0), "reverse");
          Collections.reverse(list);
          return null;
        }
    ));

    // Return whether a list contains a value.
    environment.define("includes", new NativeFunction("includes", 2,
        (interpreter, args) -> {
          List<Object> list = requireList(args.get(0), "includes");
          return list.contains(args.get(1));
        }
    ));

  }

  private static List<Object> requireList(
      Object value, String function) {

    if (value instanceof List<?>) {
      @SuppressWarnings("unchecked")
      List<Object> list = (List<Object>) value;
      return list;
    }

    throw new RuntimeError(null,
        function + "() expects a list.");
  }
}
