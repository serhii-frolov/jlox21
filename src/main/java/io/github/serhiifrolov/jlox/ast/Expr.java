package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;

import java.util.Objects;

/**
 * Expression nodes of the Lox syntax tree.
 *
 * <p>A closed set: every pass over the tree is a pattern-matching {@code switch} that the
 * compiler checks for exhaustiveness, so adding a node type here breaks every pass that
 * does not handle it. Keep {@code default} branches out of those switches or the check is lost.
 *
 * <p>Every node carries the {@link Span} of the source it was parsed from. Children and spans
 * are never {@code null}; the one permitted {@code null} is {@link Literal#value()}, which
 * encodes Lox {@code nil}.
 *
 * <p>{@code permits} is redundant for nested subtypes in the same file; kept as documentation.
 */
public sealed interface Expr permits Expr.Binary, Expr.Grouping, Expr.Literal, Expr.Unary {

    Span span();

    record Binary(Expr left, Token operator, Expr right, Span span) implements Expr {
        public Binary {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(span, "span");
        }
    }

    record Grouping(Expr expression, Span span) implements Expr {
        public Grouping {
            Objects.requireNonNull(expression, "expression");
            Objects.requireNonNull(span, "span");
        }
    }

    /** {@code value} is a {@code Double}, {@code String}, {@code Boolean}, or {@code null} for {@code nil}. */
    record Literal(Object value, Span span) implements Expr {
        public Literal {
            Objects.requireNonNull(span, "span");
        }
    }

    record Unary(Token operator, Expr right, Span span) implements Expr {
        public Unary {
            Objects.requireNonNull(operator, "operator");
            Objects.requireNonNull(right, "right");
            Objects.requireNonNull(span, "span");
        }
    }
}
