package com.company.fail;
import java.util.HashMap;
import java.util.Map;

class Instance {
    private FailClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    Instance(FailClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        Function method = klass.findMethod(this, name.lexeme);
        if (method != null) return method;

        throw new RuntimeError(name,
                "Undefined property '" + name.lexeme + "'.");
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }
}