package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;
import io.github.serhiifrolov.jlox.lexer.TokenType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ExprTest {
    private static final Span S = new Span(0, 0, 1, 1);
    private static final Token PLUS = new Token(TokenType.PLUS, "+", null, S);
    private static final Expr ONE = new Expr.Literal(1.0, S);

    @Test
    void childrenAndSpanAreNonNull() {
        assertThatNullPointerException().isThrownBy(() -> new Expr.Binary(null, PLUS, ONE, S)).withMessage("left");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Binary(ONE, null, ONE, S)).withMessage("operator");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Binary(ONE, PLUS, null, S)).withMessage("right");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Binary(ONE, PLUS, ONE, null)).withMessage("span");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Grouping(null, S)).withMessage("expression");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Grouping(ONE, null)).withMessage("span");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Unary(null, ONE, S)).withMessage("operator");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Unary(PLUS, null, S)).withMessage("right");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Unary(PLUS, ONE, null)).withMessage("span");
        assertThatNullPointerException().isThrownBy(() -> new Expr.Literal(1.0, null)).withMessage("span");
    }

    @Test
    void literalValueMayBeNullForNil() {
        assertThat(new Expr.Literal(null, S).value()).isNull();
    }

    @Test
    void equalityIncludesSpan() {
        var other = new Span(5, 1, 2, 3);
        assertThat(new Expr.Literal(1.0, S)).isEqualTo(new Expr.Literal(1.0, S));
        assertThat(new Expr.Literal(1.0, S)).isNotEqualTo(new Expr.Literal(1.0, other));
    }
}
