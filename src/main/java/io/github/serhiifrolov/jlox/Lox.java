package io.github.serhiifrolov.jlox;

import io.github.serhiifrolov.jlox.lexer.Scanner;
import io.github.serhiifrolov.jlox.lexer.Token;
import java.util.List;

/** Pure pipeline: source in, result out. No I/O, no System.exit, no global state. */
public final class Lox {
    private Lox() {}

    public static RunResult run(String source) {
        var scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        return new RunResult(tokens, scanner.diagnostics());
    }
}
