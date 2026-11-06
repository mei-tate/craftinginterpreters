# Crafting Interpreters

This repository contains my implementation of the Lox programming language from Robert Nystrom's book *Crafting Interpreters*. I have extended the original language with additional features and modifications, which are documented below in this README.

Bookmark:
https://craftinginterpreters.com/inheritance.html

## How to Run jlox
From the `/interpreter` directory path run the following commands.

Generate AST:
```
javac -d out tools/GenerateAst.java
java -cp out tools.GenerateAst jlox
```

Compile source files:
```java
javac -cp "lib/jline-4.4.8.jar" -d out jlox/*.java tools/*.java
```

The -d out option places the compiled .class files in the out/ directory.

Run Interpreter:
```java
java -cp "out:lib/jline-4.4.8.jar" jlox.Lox
```

To run a test script:
```java
java -cp "out:lib/jline-4.4.8.jar" jlox.Lox tests/test.lox
```

To run the linter:
```java
 java -jar lib/google-java-format-2.0.0-all-deps.jar --replace jlox/*.java
```

### Helpful Resources:
- Link to [Robert Nystrom's Crafting Interpreters Book Online](https://craftinginterpreters.com/).
- Lox follows [C's precedence rules](https://en.cppreference.com/c/language/operator_precedence#cite_note-3).
- My expansion of lox's grammar can be read in [grammar.md](grammar.md).


### Ideas to Extend Lox:
- Allowing comparison between strings
  - For example, "apple" < "banana" would evaluate to true (similar to Python)
- Add more native functions: reading input from the user, working with files, etc.
- Built-in data structures like lists and maps, etc.
- A core library for file I/O, user input, etc. 
- We have methods on instances, but there is no way to define “static” methods that can be called directly on the class object itself. Add support for them. Use a class keyword preceding the method to indicate a static method that hangs off the class object.
  ```
  class Math {
    class square(n) {
      return n * n;
    }
  }
  print Math.square(3); // Prints "9".
  ```
  You can solve this however you like, but the “metaclasses” used by Smalltalk and Ruby are a particularly elegant approach. Hint: Make LoxClass extend LoxInstance and go from there.
- Most modern languages support “getters” and “setters”—members on a class that look like field reads and writes but that actually execute user-defined code. Extend Lox to support getter methods. These are declared without a parameter list. The body of the getter is executed when a property with that name is accessed.
  ```
  class Circle {
    init(radius) {
      this.radius = radius;
    }

    area {
      return 3.141592653 * this.radius * this.radius;
    }
  }

  var circle = Circle(4);
  print circle.area; // Prints roughly "50.2655".
  ```


### Additions I added into Lox from the books Challenges and Suggestions:
- AstPrinter implements visitor methods for the entire grammar, i.e., AstPrinter class implements `Expr.Visitor<String>` and `Stmt.Visitor<String>`.
- Break statements: Added `break;` to exit the nearest enclosing loop. Using `break` outside a loop causes a syntax error. Also ensured break statements are only used inside loops in Resolver.
- Block comments: Added support for C-style `/* ... */` comments, including newlines.
- Comma operator: Added comma-separated expressions. Each expression is evaluated in order, and the final expression's value is returned.
- Ternary operator: Added the conditional `?:` operator with right-associative behavior.
- Binary operator errors: Added syntax error handling for binary operators that appear without a left operand.
- String concatenation: Extended `+` to concatenate strings with other values by converting non-string operands to strings.
- Division by zero: Added runtime error handling for division by zero.
- Anonymous functions (lambdas)
- Reports warnings for code that isn’t necessarily wrong but isn’t useful: unreachable code after a return statement and locals variable whose value is never read. 
- REPL lets users type in both statements and expressions. If they enter a statement, its executed. If they enter an expression, its evaluated and display the result value.
- Extended the resolver to associate a unique index for each local variable declared in a scope. When resolving a variable access, we look up both the scope the variable is in and its index and store that. In the interpreter, we use that to quickly access a variable by its index instead of using a map.

### Other Additions I added:
- Improved REPL using JLine: 
  - Up arrow: previous command.
  - Down arrow: next command.
  - Left/right arrows: move through the current line.
  - Ctrl+D exits the REPL.
  - Ctrl+C clears the current input.

