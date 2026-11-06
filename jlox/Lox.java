package jlox;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/** Lox */
public class Lox {
  // static so that successive calls to run() inside a REPL session reuse the same interpreter.
  private static final Interpreter interpreter = new Interpreter();
  static boolean hadError = false;
  static boolean hadRuntimeError = false;

  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64);
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  /**
   * When interpreter is run from the command line and given a path to a file, it reads the file and
   * executes it.
   *
   * @param path
   * @throws IOException
   */
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    run(new String(bytes, Charset.defaultCharset()), false);

    // If the user is running a Lox script from a file and a runtime error occurs, we set an exit
    // code when the process quits to let the calling process know.
    if (hadError) System.exit(65);
    if (hadRuntimeError) System.exit(70);
  }

  /**
   * Runs interpreter interactively (REPL)
   *
   * @throws IOException
   */
  private static void runPrompt() throws IOException {
    Terminal terminal = TerminalBuilder.builder().system(true).build();

    LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

    while (true) {
      String line;

      try {
        line = reader.readLine("> ");
      } catch (UserInterruptException e) {
        // Ctrl+C clears the current input.
        continue;
      } catch (EndOfFileException e) {
        // Ctrl+D exits the REPL.
        break;
      }

      run(line, true);
      hadError = false;
    }

    terminal.close();
  }

  /**
   * @param source
   */
  private static void run(String source, boolean prompt) {
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    List<Stmt> statements = prompt ? parser.parsePrompt() : parser.parse();

    // Stop if there was a syntax error.
    if (hadError) return;

    Resolver resolver = new Resolver(interpreter);
    resolver.resolve(statements);

    // Stop if there was a resolution error.
    if (hadError) return;

    if (prompt) interpreter.interpretPrompt(statements);
    else interpreter.interpret(statements);

    // System.out.println(new AstPrinter().print(expression));
  }

  /**
   * @param line
   * @param message
   */
  static void error(int line, String message) {
    report(line, "", message);
  }

  static void warning(Token token, String message) {
    String location = token == null ? "" : "[line " + token.line + "] ";
    System.err.println(location + "Warning: " + message);
  }

  /**
   * @param line
   * @param where
   * @param message
   */
  private static void report(int line, String where, String message) {
    System.err.println("[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }

  /** Reports an error at a given token. Shows the token’s location and the token itself. */
  static void error(Token token, String message) {
    if (token.type == TokenType.EOF) {
      report(token.line, " at end", message);
    } else {
      report(token.line, " at '" + token.lexeme + "'", message);
    }
  }

  /**
   * Use the token associated with the RuntimeError to tell the user what line of code was executing
   * when the error occurred.
   */
  static void runtimeError(RuntimeError error) {
    System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
    hadRuntimeError = true;
  }
}
