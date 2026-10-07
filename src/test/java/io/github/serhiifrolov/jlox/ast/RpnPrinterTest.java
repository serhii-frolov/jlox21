package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.lexer.TokenType;
import org.junit.jupiter.api.Test;
import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;

import static org.assertj.core.api.Assertions.assertThat;

class RpnPrinterTest {
    private static final Span S = new Span(0, 0, 1, 1);
    private static Token op(TokenType t, String lexeme) { return new Token(t, lexeme, null, S); }
    @Test
    void bookExample() {                       // (1 + 2) * (4 - 3)
        var expr = new Expr.Binary(
            new Expr.Grouping(new Expr.Binary(new Expr.Literal(1.0, S), op(TokenType.PLUS, "+"), new Expr.Literal(2.0, S), S), S),
            op(TokenType.STAR, "*"),
            new Expr.Grouping(new Expr.Binary(new Expr.Literal(4.0, S), op(TokenType.MINUS, "-"), new Expr.Literal(3.0, S), S), S),
            S);
        assertThat(RpnPrinter.print(expr)).isEqualTo("1.0 2.0 + 4.0 3.0 - *");
    }

    @Test
    void unaryMinusIsDistinctFromSubtraction() {   // -123 * 2
        var expr = new Expr.Binary(
            new Expr.Unary(op(TokenType.MINUS, "-"), new Expr.Literal(123.0, S), S),
            op(TokenType.STAR, "*"),
            new Expr.Literal(2.0, S),
            S);
        assertThat(RpnPrinter.print(expr)).isEqualTo("123.0 ~ 2.0 *");
    }
}
