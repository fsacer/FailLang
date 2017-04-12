package com.company.fail;

/**
 * Created by franci on 12.4.2017.
 */
class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}