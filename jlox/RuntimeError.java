package jlox;

/**
 * RuntimeError Tracks the token that identifies where in the user’s code the runtime error came
 * from.
 */
public class RuntimeError extends RuntimeException {
  final Token token;

  public RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }
}
