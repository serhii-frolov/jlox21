package io.github.serhiifrolov.jlox.lexer;

import io.github.serhiifrolov.jlox.Span;

/** {@code literal} is null except for STRING (String) and NUMBER (Double). */
public record Token(TokenType type, String lexeme, Object literal, Span span) {
    @Override
    public String toString() {
        return type + " '" + lexeme + "'" + (literal != null ? " " + literal : "") + " @" + span;
    }
}
