package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;

public sealed interface Expr permits Expr.Binary, Expr.Grouping, Expr.Literal, Expr.Unary {

    Span span();

    record Binary(Expr left, Token operator, Expr right, Span span) implements Expr {}
    record Grouping(Expr expression, Span span) implements Expr {}
    record Literal(Object value, Span span) implements Expr {}
    record Unary(Token operator, Expr right, Span span) implements Expr {}
}



