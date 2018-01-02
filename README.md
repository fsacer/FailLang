# Fail language

Language based on lox and tweaked from book "Crafting Interpreters" by @munificent.

## Properties
- regular grammar
- recursive descent parser

## Keywords
### Constructs
- if, else
- for, while, do-while (break, continue)
- fun
- class (this, super)

### Data types
- var
- Boolean: true, false, and, or
- none

### Misc
- print

## Grammar
Statements:
   
    program      → declaration* EOF ;
    declaration  → classDecl
                 | funDecl
                 | varDecl
                 | statement ;
    classDecl    → "class" IDENTIFIER ( "<" IDENTIFIER )?
                   "{" function* "}" ;
    funDecl      → "fun" function ;
    varDecl      → "var" IDENTIFIER ( "=" expression )? ";" ;
    statement    → exprStmt
                 | forStmt
                 | ifStmt
                 | printStmt
                 | returnStmt
                 | whileStmt
                 | block;
    exprStmt     → expression ";" ;
    forStmt      → "for" "(" ( varDecl | exprStmt | ";" )
                             expression? ";"
                             expression? ")" statement ;
    ifStmt       → "if" "(" expression ")" statement ( "else" statement )? ;
    printStmt    → "print" expression ";" ;
    doWhileStmt  → "do" statement "while" "(" expression ")" ";" ;
    whileStmt    → "while" "(" expression ")" statement ;
    breakStmt    → "break" ";" ;
    continueStmt → "continue" ";" ;
    block        → "{" declaration* "}" ;

Expressions:

    expression  → comma ;
    comma       → assignment ( "," assignment )*
    assignment  → ( call "." )? ( ( "=" | "+=" | "-=" | "*=" | "/=" | "**=" ) assignment )?
                | ternary ;
    ternary     → logic_or ( "?" expression ":" ternary )?
    logic_or    → logic_and ( "or" logic_and )*
    logic_and   → equality ( "and" equality )*
    equality    → comparison ( ( "!=" | "==" ) comparison )*
    comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    term        → factor ( ( "-" | "+" ) factor )*
    factor      → unary ( ( "/" | "*" ) unary )*
    unary       → ( "!" | "-" | "++" | "--" ) unary
                | exponent ;
    exponent    → (prefix "**" unary)
                | prefix ;
    prefix      → ("++" | "--") primary
                | postfix ;
    postfix     → primary ( "++" | "--" )* 
                | call ; 
    call        → primary ( "(" arguments? ")" | "." IDENTIFIER )* ;
    primary     → "true" | "false" | "none" | "this"
                | NUMBER | STRING | IDENTIFIER | "(" expression ")"
                | lambda
                | "super" "." IDENTIFIER
                // Error productions...
                | ( "!=" | "==" ) equality
                | ( ">" | ">=" | "<" | "<=" ) comparison
                | ( "+" ) term
                | ( "/" | "*" ) factor
                | ("**") exponent;
                
Other:

    arguments    → expression ( "," expression )* ;
    parameters   → IDENTIFIER ( "," IDENTIFIER )* ;
    function     → IDENTIFIER functionBody;
    method       → function;
    static method→ "class" method;
    constructor  → "init" functionBody; # returns this
    functionBody → "(" parameters? ")" block ;
    lambda       → "fun" functionBody ;
               
### Notes
Unary '+' operator is not supported.

Currently continue statement does not work as expected for for loops, incrementors must be manually incremented.

## Rules
### Operator precedence (highest → lowest)

    Name	      Operators	               Associates
    Call          a()                      Left
    Postfix       a++ a--                  Left
    Prefix        ++a --a                  Right
    Exponent      **                       Right
    Unary	      ! -                      Right
    Factor	      / *                      Left
    Term	      - +                      Left
    Comparison    > >= < <=	               Left
    Equality      == !=                    Left
    Logical And   and                      Left
    Logical Or    or                       Left
    Ternary       ?:                       Right
    Assignment    =, +=, -=, /=, *=, **=   Right
    Comma         ,                        Left

### Truthyness
Fail follows Ruby’s simple rule: false and none are falsey and everything else is truthy.

## Escape sequences
    \" – double quote
    \\ – single backslash
    \b – backspace
    \r – carriage return
    \n – newline
    \t – tab

## Data types
Variables can change it's data type at runtime as in Python. Data types are implied from expressions. 
### Built-in data types
- Boolean: true or false
- string
- number (`double` precision)
- none - a null pointer
### User defined data types
Those can be defined via `class` keyword.

## Standard library
### Global functions
- `clock()` - Gets the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC. Then it changes milliseconds to seconds as a floating point value.
- `len(x)` - This function changes input to a string and gets its length.
- `str(x)` - Changes input to its string representation.

## Added features
Additional features mostly based on tasks from book:
- multiline comments
- postfix and prefix increment/decrement operators
- ternary operator
- exponent operator
- prevent access to unassigned variables (no implicit initialization to none)
- accept escape sequences
- comma operator
- operator overload for string multiplication ("abc" * 2 → "abcabc")
- break and continue
- do-while statement
- shorthand assignment operators +=, -=, *=, /=, **=
- prevention of assignment inside if, loop and ternary condition expressions
- lambdas
- warnings, if local variable is unused
- static methods, getters and setters