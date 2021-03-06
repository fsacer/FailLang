package com.company.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.company.fail.TokenType.*;

class Scanner {
    private final StringBuilder source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("none", NONE);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("do", DO);
        keywords.put("while", WHILE);
        keywords.put("break", BREAK);
        keywords.put("continue", CONTINUE);
    }

    Scanner(String source) {
        this.source = new StringBuilder(source);
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(':
                addToken(LEFT_PAREN);
                break;
            case ')':
                addToken(RIGHT_PAREN);
                break;
            case '{':
                addToken(LEFT_BRACE);
                break;
            case '}':
                addToken(RIGHT_BRACE);
                break;
            case ',':
                addToken(COMMA);
                break;
            case '.':
                addToken(DOT);
                break;
            case '-':
                addToken(match('-') ? MINUS_MINUS :
                        match('=') ? MINUS_EQUAL :
                                MINUS);
                break;
            case '+':
                addToken(match('+') ? PLUS_PLUS :
                        match('=') ? PLUS_EQUAL :
                                PLUS);
                break;
            case ';':
                addToken(SEMICOLON);
                break;
            case ':':
                addToken(COLON);
                break;
            case '?':
                addToken(QUESTION_MARK);
                break;
            case '*':
                addToken(match('*') ?
                        match('=') ? STAR_STAR_EQUAL : STAR_STAR :
                        match('=') ? STAR_EQUAL : STAR);
                break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line.
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    int toMatch = 1;
                    while (!isAtEnd()) {
                        if (peek() == '*' && peekNext() == '/') toMatch--;
                        if (peek() == '/' && peekNext() == '*') toMatch++;
                        if (peek() == '\n') line++;
                        if (toMatch == 0) break;
                        advance();
                    }
                    if (peek() == '*' && peekNext() == '/') {
                        advance();
                        advance();
                    } else {
                        Fail.error(line, "No closing of block comment.");
                    }
                } else if (match('=')) {
                    addToken(SLASH_EQUAL);
                } else {
                    addToken(SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;

            case '\n':
                line++;
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Fail.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (current >= source.length()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            char first = peek();
            if (first == '\n') line++;

            //handle escape sequences
            if(first == '\\'){
                char second = peekNext();
                if (second == '\"') {
                    source.deleteCharAt(current);
                    source.setCharAt(current, '\"');
                } else if (second == '\\') {
                    source.deleteCharAt(current);
                    source.setCharAt(current, '\\');
                } else if (second == 'b') {
                    source.deleteCharAt(current);
                    source.setCharAt(current, '\b');
                } else if (second == 'r') {
                    source.deleteCharAt(current);
                    source.setCharAt(current, '\r');
                } else if (second == 'n') {
                    source.deleteCharAt(current);
                    source.setCharAt(current, '\n');
                } else if (second == 't') {
                    source.deleteCharAt(current);
                    source.setCharAt(current, '\t');
                }
            }
            advance();
        }

        // Unterminated string.
        if (isAtEnd()) {
            Fail.error(line, "Unterminated string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        // See if the identifier is a reserved word.
        String text = source.substring(start, current);

        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}