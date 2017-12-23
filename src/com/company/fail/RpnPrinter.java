package com.company.fail;

class RpnPrinter implements Expr.Visitor<String> {
    String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return postfix(expr.name + " =", expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return postfix(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitFunctionExpr(Expr.Function expr) {
        return "lambda";
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        return print(expr.callee);
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "this";
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return "get " + expr.name.lexeme;
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return  "set " + expr.name.lexeme;
    }

    @Override
    public String visitTernaryExpr(Expr.Ternary expr) {
        return postfix("?", expr.expr, expr.thenBranch, expr.elseBranch);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return print(expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return postfix(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return expr.right.accept(this) + " " + expr.operator.lexeme + "u";
    }

    private String postfix(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();

        for (Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }

        builder.append(" ").append(name);

        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
                new Expr.Unary(
                        new Token(TokenType.MINUS, "-", null, 1),
                        new Expr.Literal(123),
                        false),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Grouping(
                        new Expr.Literal(45.67)));

        System.out.println(new RpnPrinter().print(expression));

        Expr expression2 = new Expr.Binary(
                new Expr.Assign(
                        new Token(TokenType.IDENTIFIER, "a", null, 1),
                        new Expr.Literal(1),
                        new Token(TokenType.EQUAL, "=", null, 1)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Literal("str")
        );

        System.out.println(new RpnPrinter().print(expression2));
    }
}