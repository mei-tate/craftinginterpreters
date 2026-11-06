package jlox;

/** Return class is used to warn about unreachable code after return. */
class Return extends RuntimeException {
  final Object value;

  Return(Object value) {
    super(null, null, false, false);
    this.value = value;
  }
}
