package jlox;

/**
 * RuntimeError Tracks the token that identifies where in the user’s code the runtime error came
 * from.
 */
class RuntimeError extends RuntimeException {
  final Token token;

  RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }
}
