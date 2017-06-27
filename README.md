# Fail language

Language based on lox and tweaked from book "Crafting Interpreters" by @munificent.

## Properties
- regular grammar
- recursive descent parser

## Keywords
### Constructs
- if, else
- for, while
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
   
    program     → declaration* EOF ;
    declaration → varDecl
                | statement ;
    varDecl     → "var" IDENTIFIER ( "=" expression )? ";" ;
    statement   → exprStmt
                | forStmt
                | ifStmt
                | printStmt
                | whileStmt
                | block;
    exprStmt    → expression ";" ;
    forStmt     → "for" "(" ( varDecl | exprStmt | ";" )
                            expression? ";"
                            expression? ")" statement ;
    ifStmt      → "if" "(" expression ")" statement ( "else" statement )? ;
    printStmt   → "print" expression ";" ;
    whileStmt   → "while" "(" expression ")" statement ;
    block       → "{" declaration* "}" ;

Expressions:

    expression  → comma ;
    comma       → assignment ( "," assignment )?
                | assignment ;
    assignment  → identifier ( "=" assignment )?
                | ternary ;
    ternary     → logic_or ? true logic_or : false logic_or )*
                | logic_or ;
    logic_or    → logic_and ( "or" logic_and )*
    logic_and   → equality ( "and" equality )*
    equality    → comparison ( ( "!=" | "==" ) comparison )*
    comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    term        → factor ( ( "-" | "+" ) factor )*
    factor      → exponent ( ( "/" | "*" ) exponent )*
    exponent    → unary ( ( "**" ) unary )*
    unary       → ( "!" | "-" | "+" ) unary
                | ( "++" | "--" ) unary
                | postfix ;
    postfix     → postfix ( "++" | "--" )
                | primary ;
    primary     → "true" | "false" | "none" | "this"
                | NUMBER | STRING
                | "(" expression ")"
                | IDENTIFIER ;
               
### Notes
Unary '+' operator is a syntax error.

## Rules
### Operator precedence (highest → lowest)

    Name	      Operators	     Associates
    Postfix       a++ a--        Left
    Unary	      ! - ++a --a    Right
    Exponent      **             Left
    Factor	      / *            Left
    Term	      - +            Left
    Comparison    > >= < <=	     Left
    Equality      == !=          Left
    Logical And   and            Left
    Logical Or    or             Left
    Ternary       ?:             Left as PHP
    Assignment    =              Right
    Comma         ,              Left

### Truthyness
Fail follows Ruby’s simple rule: false and none are falsey and everything else is truthy.

## Escape sequences
    \" – double quote
    \\ – single backslash
    \b – backspace
    \r – carriage return
    \n – newline
    \t – tab

## Added features
Additional features mostly based on tasks from book:
- multiline comments
- postfix and prefix increment/decrement operators
- ternary operator
- exponent operator
- prevent access to unassigned variables (no implicit initialization to none)
- changed the order of var assignment (first definition of variable with none and then assignment)
  to prevent accessing variable from outer scope during initializing
- accept escape sequences
- added comma operator