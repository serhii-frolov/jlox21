package io.github.serhiifrolov.jlox;

import io.github.serhiifrolov.jlox.lexer.Token;
import java.util.List;

/** Everything {@link Lox#run} produced for one source text. Both lists are immutable copies. */
public record RunResult(List<Token> tokens, List<Diagnostic> diagnostics) {
    public RunResult {
        tokens = List.copyOf(tokens);
        diagnostics = List.copyOf(diagnostics);
    }

    public boolean hasErrors() {
        return !diagnostics.isEmpty();
    }
}
