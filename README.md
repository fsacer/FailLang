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
    program     → declaration* EOF ;
    declaration → varDecl
                | statement ;
    varDecl     → "var" IDENTIFIER ( "=" expression )? ";" ;
    statement   → exprStmt
                | printStmt
                | block;
    exprStmt    → expression ";" ;
    printStmt   → "print" expression ";" ;
    block       → "{" declaration* "}" ;
     
    expression  → assignment ;
    assignment  → identifier ( "=" assignment )?
                | ternary ;
    ternary     → equality ? true equality : false equality )* | equality
    equality    → comparison ( ( "!=" | "==" ) comparison )*
    comparison  → term ( ( ">" | ">=" | "<" | "<=" ) term )*
    term        → factor ( ( "-" | "+" ) factor )*
    factor      → exponent ( ( "/" | "*" ) exponent )*
    exponent    → unary ( ( "**" ) unary )*
    unary       → ( "!" | "-" | "+" ) unary
                | ( "++" | "--" ) unary
                | postfix
    postfix     → postfix ( "++" | "--" )
                | primary
    primary     → "true" | "false" | "none" | "this"
                | NUMBER | STRING
                | "(" expression ")"
                | IDENTIFIER ;
               
### Notes
Unary '+' operator is a syntax error.

##Rules
###Truthyness
Fail follows Ruby’s simple rule: false and none are falsey and everything else is truthy. We implement that like so:

## Added features
Additional features mostly based on tasks from book:
- multiline comments
- postfix and prefix increment/decrement operators
- ternary operator
- exponent operator
- prevent access to unassigned variables (no implicit initialization to none)
- changed the order of var assignment (first definition of variable with none and then assignment)
  to prevent accessing variable from outer scope during initializing
