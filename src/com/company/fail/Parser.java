package com.company.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.company.fail.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    private int loopLevel = 0;

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
            List<Stmt> declarations = declarations();
            if (declarations != null)
                statements.addAll(declarations);
        }

        return statements;
    }

    private Expr expression() {
        return comma();
    }

    private List<Stmt> declarations() {
        List<Stmt> lst = new ArrayList<>();
        try {
            if (match(VAR)) lst.addAll(varDeclarations());
            else lst.add(statement());

            return lst;
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(DO)) return doWhileStatement();
        if (match(WHILE)) return whileStatement();
        if (match(BREAK)) return breakStatement();
        if (match(CONTINUE)) return continueStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    //using desugaring technique
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        List<Stmt> initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclarations();
        } else {
            initializer = new ArrayList<>();
            initializer.add(expressionStatement());
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        try {
            loopLevel++;
            Stmt body = statement();

            if (increment != null) {
                body = new Stmt.Block(Arrays.asList(
                        body,
                        new Stmt.Expression(increment)));
            }

            if (condition == null) condition = new Expr.Literal(true);
            body = new Stmt.While(condition, body);

            if (initializer != null) {
                List<Stmt> stmts = new ArrayList<>();
                stmts.addAll(initializer);
                stmts.add(body);
                body = new Stmt.Block(stmts);
            }

            return body;
        } finally {
            loopLevel--;
        }
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private List<Stmt> varDeclarations() {
        List<Stmt> lst = new ArrayList<>();
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = assignment();
        }
        lst.add(new Stmt.Var(name, initializer));
        while (match(COMMA)) {
            name = consume(IDENTIFIER, "Expect variable name.");
            consume(EQUAL, "Expect assignment in multiple variable declaration.");
            initializer = assignment();
            lst.add(new Stmt.Var(name, initializer));
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return lst;
    }

    private Stmt doWhileStatement() {
        try {
            loopLevel++;
            Stmt body = statement();
            consume(WHILE, "Expect 'while' in a do-while loop.");
            consume(LEFT_PAREN, "Expect '(' after 'while'.");
            Expr condition = expression();
            consume(RIGHT_PAREN, "Expect ')' after condition.");
            consume(SEMICOLON, "Expect ';' after do-while statement.");

            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.While(condition, body)));

            return body;
        } finally {
            loopLevel--;
        }
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");

        try {
            loopLevel++;
            Stmt body = statement();

            return new Stmt.While(condition, body);
        } finally {
            loopLevel--;
        }
    }

    private Stmt breakStatement() {
        if (!(loopLevel > 0)) throw error(previous(), "Break statement must be inside a loop.");
        consume(SEMICOLON, "Expect ';' after 'break' statement.");
        return new Stmt.Break();
    }

    private Stmt continueStatement() {
        if (!(loopLevel > 0)) throw error(previous(), "Continue statement must be inside a loop.");
        consume(SEMICOLON, "Expect ';' after 'continue' statement.");
        return new Stmt.Continue();
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            List<Stmt> declarations = declarations();
            if (declarations != null) {
                statements.addAll(declarations);
            }
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr comma() {
        Expr expr = assignment();

        while (match(COMMA)) {
            Token comma = previous();
            Expr right = assignment();
            expr = new Expr.Binary(expr, comma, right);
        }

        return expr;
    }

    private Expr assignment() {
        Expr expr = ternary();

        if (match(EQUAL, PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL, STAR_STAR_EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value, equals);
            }

            throw error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr ternary() {
        Expr expr = or();

        if (match(QUESTION_MARK)) {
            Expr thenBranch = expression();
            consume(COLON, "Expect ':' after then branch of ternary expression.");
            Expr elseBranch = ternary();
            expr = new Expr.Ternary(expr, thenBranch, elseBranch);
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();

        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        Expr expr = equality();

        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
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

    private Expr exponent() {
        Expr expr = unary();

        while (match(STAR_STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS, PLUS_PLUS, MINUS_MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right, false);
        }

        return postfix();
    }

    private Expr postfix() {
        if (matchNext(PLUS_PLUS, MINUS_MINUS)) {
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

        // Error productions.
        if (match(QUESTION_MARK)) {
            throw error(previous(), "Missing left-hand condition of ternary operator.");
        }
        if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            throw error(previous(), "Missing left-hand operand.");
        }
        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            throw error(previous(), "Missing left-hand operand.");
        }
        if (match(PLUS)) {
            throw error(previous(), "Missing left-hand operand.");
        }
        if (match(SLASH, STAR, STAR_STAR)) {
            throw error(previous(), "Missing left-hand operand.");
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
                case BREAK:
                case CONTINUE:
                    return;
            }

            advance();
        }
    }
}
