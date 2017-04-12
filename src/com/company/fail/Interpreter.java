package com.company.fail;

/**
 * Created by franci on 12.4.2017.
 */
import static java.lang.Math.*;
class Interpreter implements Expr.Visitor<Object> {
    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Fail.runtimeError(error);
        } catch (Exception error) {
            //Do not crash
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTrue(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case PLUS_PLUS: {
                checkNumberOperand(expr.operator, right);
                double value = (double) right;
                if (expr.postfix)
                    return value;
                else
                    return value + 1;
            }
            case MINUS_MINUS: {
                checkNumberOperand(expr.operator, right);
                double value = (double) right;
                if (expr.postfix)
                    return value;
                else
                    return value - 1;
            }
        }

        // Unreachable.
        return null;
    }

    private boolean isTrue(Object object) {
        return object != null && (!(object instanceof Boolean) || (boolean) object);
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

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                return (double)left <= (double)right;
            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }

                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
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
                //if((double) right == 0) throw new RuntimeError(expr.operator, "Division by zero not allowed.");
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            case STAR_STAR:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((double)left, (double)right);
        }

        // Unreachable.
        return null;
    }

    private boolean isEqual(Object a, Object b) {
        // none is only equal to none.
        return a == null && b == null || a != null && a.equals(b);
    }


    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object check = evaluate(expr.expr);
        Object trueExpr = evaluate(expr.trueExpr);
        Object falseExpr = evaluate(expr.falseExpr);

        if(isTrue(check))
            return trueExpr;
        else
            return falseExpr;
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

}