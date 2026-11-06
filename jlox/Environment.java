package jlox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Environment {
  final Environment enclosing;
  private final List<Object> values = new ArrayList<>();
  private final Map<String, Object> globals = new HashMap<>();

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) {
    if (globals.containsKey(name.lexeme)) return globals.get(name.lexeme);
    if (enclosing != null) return enclosing.get(name);
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (globals.containsKey(name.lexeme)) {
      globals.put(name.lexeme, value);
      return;
    }
    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void define(String name, Object value) {
    globals.put(name, value);
  }

  void defineAt(int slot, Object value) {
    while (values.size() <= slot) values.add(null);
    values.set(slot, value);
  }

  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing;
    }
    return environment;
  }

  Object getAt(int distance, int slot) {
    return ancestor(distance).values.get(slot);
  }

  void assignAt(int distance, int slot, Object value) {
    ancestor(distance).values.set(slot, value);
  }
}
