# Native functions

Native functions are built into the interpreter and can be called like ordinary Lox functions. Their arguments are evaluated before the native runs. A call with the wrong number of arguments reports a runtime error.

```lox
> print size();
Expected 1 arguments but got 0.
[line 1]
```

The interpreter currently provides the following native functions.

## Time and system

| Function | Use |
| --- | --- |
| `clock()` | Current Unix time in seconds, as a number. |
| `time()` | Current Unix time in seconds, as a number. |
| `timeMillis()` | Current Unix time in milliseconds, as a number. |
| `sleep(seconds)` | Pauses for a non-negative number of seconds. Returns `nil`. |

`clock()` and `time()` both return the current Unix timestamp in seconds.

```lox
var started = timeMillis();
sleep(0.25);
print timeMillis() - started;
```

## Input and output

| Function | Use |
| --- | --- |
| `input()` | Reads and returns the next line from standard input as a string. |

Example:

```lox
print "What is your name?";
var name = input();
print "Hello, " + name + "!";
```

Use Lox's `print` statement to display values; `print` is a statement, not a native function.

## Types

| Function | Use |
| --- | --- |
| `type(value)` | Returns `"nil"`, `"number"`, `"string"`, `"boolean"`, `"function"`, or `"object"`. |

```lox
print type(42);       // number
print type("hello"); // string
print type(list());   // object
```

## Strings

All string functions require string arguments. String indexes are zero-based; `substring` uses an exclusive end index.

| Function | Use |
| --- | --- |
| `len(text)` | Number of characters in the string. |
| `lower(text)` | Lowercase copy. |
| `upper(text)` | Uppercase copy. |
| `trim(text)` | Copy with leading and trailing whitespace removed. |
| `contains(text, part)` | Whether `text` contains `part`. |
| `startsWith(text, prefix)` | Whether `text` starts with `prefix`. |
| `endsWith(text, suffix)` | Whether `text` ends with `suffix`. |
| `replace(text, old, replacement)` | Copy with every occurrence of `old` replaced. |
| `substring(text, start, end)` | Slice from `start` up to, but not including, `end`. Requires valid integer indexes. |

```lox
print len("lox");                       // 3
print upper(trim("  hello  "));         // HELLO
print replace("one fish", "fish", "bird");
print substring("interpreter", 0, 5);   // inter
```

## Lists and collections

`list()` creates a mutable empty list. List operations mutate the list where indicated. `size` also accepts a Java map value if one is provided by another native or host integration; Lox currently has no map literal or map constructor.

| Function | Use |
| --- | --- |
| `list()` | Creates an empty list. |
| `size(collection)` | Number of elements in a list or map. |
| `push(list, value)` | Appends `value`; returns `nil`. |
| `pop(list)` | Removes and returns the last value. Errors if the list is empty. |
| `reverse(list)` | Reverses the list in place; returns `nil`. |
| `includes(list, value)` | Whether the list contains `value`. |

```lox
var numbers = list();
push(numbers, 10);
push(numbers, 20);
print size(numbers);         // 2
print includes(numbers, 10); // true
print pop(numbers);          // 20
reverse(numbers);
```

## Math

Math functions that accept numbers report a runtime error if an argument is not a number. `random()` returns a number in the range `[0, 1)`. `randomInt(min, max)` returns an integer-valued number in the inclusive range from `min` to `max`.

| Function | Use |
| --- | --- |
| `min(a, b)` | Smaller of two numbers. |
| `max(a, b)` | Larger of two numbers. |
| `floor(number)` | Greatest integer less than or equal to the number. |
| `ceil(number)` | Smallest integer greater than or equal to the number. |
| `round(number)` | Nearest integer, returned as a number. |
| `abs(number)` | Absolute value. |
| `sqrt(number)` | Square root. |
| `pow(base, exponent)` | `base` raised to `exponent`. |
| `random()` | Random number from 0 inclusive to 1 exclusive. |
| `randomInt(min, max)` | Random whole number from `min` to `max`, inclusive; both bounds must be integers and `min` cannot exceed `max`. |

```lox
print max(3, 8);             // 8
print round(3.6);            // 4
print pow(2, 3);             // 8
print randomInt(1, 6);       // 1 through 6
```
