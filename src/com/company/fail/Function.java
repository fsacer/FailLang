package com.company.fail;

import java.util.List;

public class Function implements Callable {
    private final String name;
    private final Expr.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    Function(String name, Expr.Function declaration, Environment closure, boolean isInitializer) {
        this.name = name;
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return declaration.parameters.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);

        if (declaration.parameters != null) {
            for (int i = 0; i < declaration.parameters.size(); i++) {
                environment.define(declaration.parameters.get(i).lexeme,
                        arguments.get(i));
            }
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }

        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    Function bind(Instance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new Function(name, declaration, environment, isInitializer);
    }

    public boolean isGetter() {
        return declaration.parameters == null;
    }

    @Override
    public String toString() {
        if (name == null) return "<fn>";
        return "<fn " + name + ">";
    }
}
