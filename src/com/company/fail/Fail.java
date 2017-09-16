package com.company.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Fail {
    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jfail [script]");
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (; ; ) {
            hadError = false;

            System.out.print("> ");
            List<Token> tokens = runScanner(reader.readLine());
            Parser parser = new Parser(tokens);

            if (tokens.stream().filter(
                    x -> x.type == TokenType.SEMICOLON
                            || x.type == TokenType.CLASS
                            || x.type == TokenType.FUN
                            || x.type == TokenType.VAR
                            || x.type == TokenType.FOR
                            || x.type == TokenType.IF
                            || x.type == TokenType.WHILE
                            || x.type == TokenType.BREAK
                            || x.type == TokenType.CONTINUE
                            || x.type == TokenType.PRINT
                            || x.type == TokenType.RETURN
                            || x.type == TokenType.LEFT_BRACE
                            || x.type == TokenType.RIGHT_BRACE).count() == 0) {
                Expr expr = parser.parseExpr();
                if (hadError) continue;
                interpreter.interpret(expr);
            } else {
                List<Stmt> statements = parser.parse();
                if (hadError) continue;
                runResolver(statements);
                if (hadError) continue;
                interpreter.interpret(statements);
            }
        }
    }

    private static void run(String source) {
        List<Token> tokens = runScanner(source);
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Stop if there was a syntax error.
        if (hadError) return;

        runResolver(statements);

        // Stop if there was a resolution error.
        if (hadError) return;

        interpreter.interpret(statements);

/*
        //Print AST
        if (!hadError) {
            System.out.println(new AstPrinter().print(expression));
        }
*/

/*
        // For now, just print the tokens.
        for (Token token : tokens) {
            System.out.println(token);
        }
*/
    }

    private static List<Token> runScanner(String source) {
        Scanner scanner = new Scanner(source);
        return scanner.scanTokens();
    }

    private static void runResolver(List<Stmt> statements) {
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);
    }

    static void error(int line, String message) {
        reportError(line, "", message);
    }

    static private void reportError(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            reportError(token.line, " at end", message);
        } else {
            reportError(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static private void reportWarning(int line, String where, String message) {
        System.err.println(
                "[line " + line + "] Warning" + where + ": " + message);
        System.err.flush();
    }

    static void warning(int line, String message) {
        reportWarning(line, "", message);
    }

    static void warning(Token token, String message) {
        if (token.type == TokenType.EOF) {
            reportWarning(token.line, " at end", message);
        } else {
            reportWarning(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}
