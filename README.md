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
    ternary    → expression ? true expression : false expression | expression
    expression → equality
    equality   → comparison ( ( "!=" | "==" ) comparison )*
    comparison → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    term       → factor ( ( "-" | "+" ) factor )*
    factor     → unary ( ( "/" | "*" ) unary )*
    unary      → ( "!" | "-" | "+" ) unary
               | ( "++" | "--" ) prefix
               | primary
    postfix    → ( "++" | "--" ) postfix
    primary    → NUMBER | STRING | "false" | "true" | "nil"
               | "(" expression ")"
               
### Notes
Unary '+' operator is a syntax error.
           
## Added features
Additional features mostly based on tasks from book:
- multiline comments
- postfix and prefix increment/decrement operators
- ternary operator