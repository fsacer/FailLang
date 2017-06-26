package com.company.fail;

import java.util.ArrayList;
import java.util.List;
import static com.company.fail.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parseExpr() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            List<Stmt> declarations = declaration();
            if(declarations != null)
                statements.addAll(declarations);
        }

        return statements;
    }

    private Expr expression() {
        return comma();
    }

    private List<Stmt> declaration() {
        List<Stmt> lst = new ArrayList<>();
        try {
            if (match(VAR)) lst.addAll(varDeclaration());
            else lst.add(statement());
            return lst;
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private List<Stmt> varDeclaration() {
        List<Stmt> lst = new ArrayList<>();
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = assignment();
        }
        lst.add(new Stmt.Var(name, initializer));
        while(match(COMMA)) {
            name = consume(IDENTIFIER, "Expect variable name.");
            consume(EQUAL, "Expect assignment in multiple variable declaration.");
            initializer = assignment();
            lst.add(new Stmt.Var(name, initializer));
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return lst;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            List<Stmt> declarations = declaration();
            if(declarations != null) {
                statements.addAll(declarations);
            }
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr comma() {
        Expr last = assignment();
        while (match(COMMA)) {
            last = assignment();
        }

        return last;
    }

    private Expr assignment() {
        Expr expr = ternary();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private  Expr ternary() {
        Expr expr = equality();

        while (match(QUESTION_MARK)){
            Token operator = previous();
            Expr right = equality();
            consume(COLON, "Missing ':' in ternary expression.");
            Expr rightRight = equality();
            expr = new Expr.Ternary(operator, expr, right, rightRight);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = exponent();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = exponent();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private  Expr exponent() {
        Expr expr = unary();

        while (match(STAR_STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        //prefix ! - ++ --
        if (match(BANG, MINUS, PLUS_PLUS, MINUS_MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right, false);
        }
        if(match(PLUS)){
            throw error(previous(), "Unary '+' not supported.");
        }

        return suffix();
    }

    private Expr suffix() {
        //postfix ++ --
        if (matchNext(PLUS_PLUS, MINUS_MINUS)){
            Token operator = peek();
            current--;
            Expr left = primary();
            advance();
            return new Expr.Unary(operator, left, true);
        }

        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NONE)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if(match(QUESTION_MARK, EQUAL_EQUAL, BANG_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, MINUS, PLUS, SLASH, STAR)) {
            throw error(previous(), "Expect expression before '" + previous().lexeme + "'.");
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean matchNext(TokenType... types) {
        for (TokenType type : types) {
            if (checkNext(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }


    private boolean check(TokenType tokenType) {
        if (isAtEnd()) return false;
        return peek().type == tokenType;
    }

    private boolean checkNext(TokenType tokenType) {
        if (isAtEnd()) return false;
        return peekNext().type == tokenType;
    }


    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token peekNext() {
        return tokens.get(current + 1);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Fail.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }

            advance();
        }
    }
}
