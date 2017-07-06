package com.company.fail;

import java.util.Collections;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();
    private static Object uninitialized = new Object();

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            if (value instanceof String) {
                value = "\"" + value + "\"";
            }
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Fail.runtimeError(error);
        }
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Fail.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = uninitialized;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakJump breakJump) {
                break;
            } catch (ContinueJump continueJump) {
                //Do nothing.
            }
        }
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                return (double) left <= (double) right;
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                /* converting everything to string
                if(left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                */

                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                //code to prevent division by zero
                //if ((double) right == 0) throw new RuntimeError(expr.operator, "Division by zero not allowed.");
                return (double) left / (double) right;
            case STAR:
                if (left instanceof Double && !(right instanceof Double)) {
                    return multiplyString(stringify(right), (double) left, expr.operator);
                }
                if (!(left instanceof Double) && right instanceof Double) {
                    return multiplyString(stringify(left), (double) right, expr.operator);
                }

                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case STAR_STAR:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double) left, (double) right);
            case COMMA:
                return right;
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakJump();
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        throw new ContinueJump();
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            case PLUS_PLUS: {
                checkNumberOperand(expr.operator, right);
                double value = (double) right;

                if (expr.right instanceof Expr.Variable) {
                    Expr.Variable variable = (Expr.Variable) expr.right;
                    environment.assign(variable.name, value + 1);
                }

                if (expr.postfix)
                    return value;
                else
                    return value + 1;
            }
            case MINUS_MINUS: {
                checkNumberOperand(expr.operator, right);
                double value = (double) right;

                if (expr.right instanceof Expr.Variable) {
                    Expr.Variable variable = (Expr.Variable) expr.right;
                    environment.assign(variable.name, value - 1);
                }

                if (expr.postfix)
                    return value;
                else
                    return value - 1;
            }
        }

        // Unreachable.
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        Object value = environment.get(expr.name);
        if (value == uninitialized) {
            throw new RuntimeError(expr.name,
                    "Variable must be initialized before use.");
        }
        return value;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object check = evaluate(expr.expr);

        if (isTruthy(check))
            return evaluate(expr.trueExpr);
        else
            return evaluate(expr.falseExpr);
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator,
                                     Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isEqual(Object a, Object b) {
        // none is only equal to none.
        return a == null && b == null || a != null && a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "none";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    private String multiplyString(String s, double n, Token token) {
        if (n % 1 != 0) throw new RuntimeError(token,
                "String multiplier must be an integer.");
        int multiplier = (int) n;
        if (multiplier < 0) multiplier = 0;

        return String.join("", Collections.nCopies(multiplier, s));
    }
}