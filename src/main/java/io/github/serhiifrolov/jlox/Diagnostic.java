package io.github.serhiifrolov.jlox;

/** A reported problem (lexer, parser, later resolver). Shared across phases. */
public record Diagnostic(String message, Span span) {
    @Override
    public String toString() {
        return "[" + span + "] Error: " + message;
    }
}
