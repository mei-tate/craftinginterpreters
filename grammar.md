# Syntax Grammar for Lox
The syntactic grammar is used to parse the linear sequence of tokens into the nested syntax tree structure. It starts with the first rule that matches an entire Lox program (or a single REPL entry).
```
program        → declaration* EOF ;
```

### Declarations
A program is a series of declarations, which are the statements that bind new identifiers or any of the other statement types.
```
declaration    → classDecl
               | funDecl
               | varDecl
               | statement ;

classDecl      → "class" IDENTIFIER ( "<" IDENTIFIER )?
                 "{" function* "}" ;
funDecl        → "fun" function ;
varDecl        → "var" IDENTIFIER ( "=" expression )? ";" ;
```

### Statements and State
The remaining statement rules produce side effects, but do not introduce bindings.
```
statement      → exprStmt
               | forStmt
               | ifStmt
               | printStmt
               | returnStmt
               | whileStmt
               | breakStmt
               | block ;

exprStmt       → expression ";" ;
forStmt        → "for" "(" ( varDecl | exprStmt | ";" )
                           expression? ";"
                           expression? ")" statement ;
ifStmt         → "if" "(" expression ")" statement
               ( "else" statement )? ;
printStmt      → "print" expression ";" ;
returnStmt     → "return" expression? ";" ;
whileStmt      → "while" "(" expression ")" statement ;
breakStmt      → "break" ";" ;
block          → "{" declaration* "}" ;
```
Note that block is a statement rule, but is also used as a nonterminal in a couple of other rules for things like function bodies.

### Expressions
Expressions produce values. Lox has a number of unary and binary operators with different levels of precedence. Some grammars for languages do not directly encode the precedence relationships and specify that elsewhere. Here, we use a separate rule for each precedence level to make it explicit.

This applies the same precedence rules as C, going from lowest to highest.

```
expression     → comma ;
comma          → assignment ( "," assignment )* ;
assignment     → IDENTIFIER "=" assignment
               | conditional ;

conditional    → logic_or ( "?" expression ":" conditional )? ;
logic_or       → logic_and ( "or" logic_and )* ;
logic_and      → equality ( "and" equality )* ;
equality       → comparison ( ( "!=" | "==" ) comparison )* ;
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
term           → factor ( ( "-" | "+" ) factor )* ;
factor         → unary ( ( "/" | "*" ) unary )* ;

unary          → ( "!" | "-" ) unary | call ;
call           → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
primary        → "true" | "false" | "nil" | "this"
               | NUMBER | STRING | IDENTIFIER | "(" expression ")"
               | "super" "." IDENTIFIER ;
               | lambda

lambda        → "fun" "(" parameters? ")" block ;
```

### Utility rules
In order to keep the above rules a little cleaner, some of the grammar is split out into a few reused helper rules.
```
function       → IDENTIFIER "(" parameters? ")" block ;
parameters     → IDENTIFIER ( "," IDENTIFIER )* ;
arguments      → expression ( "," expression )* ;
```

# Lexical Grammar
The lexical grammar is used by the scanner to group characters into tokens. Where the syntax is context free, the lexical grammar is regular—note that there are no recursive rules.

```
NUMBER         → DIGIT+ ( "." DIGIT+ )? ;
STRING         → "\"" <any char except "\"">* "\"" ;
IDENTIFIER     → ALPHA ( ALPHA | DIGIT )* ;
ALPHA          → "a" ... "z" | "A" ... "Z" | "_" ;
DIGIT          → "0" ... "9" ;
```


| AST Node   | Purpose                                         | Example                 |
| ---------- | ----------------------------------------------- | ----------------------- |
| `Literal`  | Represents a fixed value                        | `42`, `"hello"`, `true` |
| `Variable` | Represents a named value that must be looked up | `x`, `count`, `name`    |


| Grammar Notation | Code Representation               |
| ---------------- | --------------------------------- |
| Terminal         | Code to match and consume a token |
| Nonterminal      | Call to that rule’s function      |
| `\\|`            | `if` or `switch` statement        |
| `*` or `+`       | `while` or `for` loop             |
| `?`              | `if` statement                    |


| Lox Type      | Java Representation |
| ------------- | ------------------- |
| Any Lox value | `Object`            |
| `nil`         | `null`              |
| Boolean       | `Boolean`           |
| Number        | `Double`            |
| String        | `String`            |




Other notes on Lox:
- Lexical scope is a specific style of scoping where the text of the program itself shows where a scope begins and ends. In Lox, variables are lexically scoped. 
- Methods and fields on objects are dynamically scoped.
- A variable statement doesn’t just define a new variable, it can also be used to redefine an existing variable.
    ```
    var a = "before";
    print a; // "before".
    var a = "after";
    print a; // "after".
    ```

- comma handles comma-separated expressions as an operator, where the entire expression evaluates to the value of the last expression.
- arguments handles comma-separated expressions passed as arguments to a function call. These are separate expressions, not a comma operator.