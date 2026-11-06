package jlox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  private LoxClass klass;
  // Each key in the map is a property name and the corresponding value is the property’s value.
  private final Map<String, Object> fields = new HashMap<>();

  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  Object get(Token name) {
    // See if the instance has a field with the given name and return it.
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

    // Look for a method with that name on the instance’s class and return it.
    LoxFunction method = klass.findMethod(name.lexeme);
    if (method != null) return method.bind(this);

    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
  }

  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  @Override
  public String toString() {
    return klass.name + " instance";
  }
}
