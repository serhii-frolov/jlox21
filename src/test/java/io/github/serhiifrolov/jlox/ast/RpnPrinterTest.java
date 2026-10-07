package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;
import io.github.serhiifrolov.jlox.lexer.TokenType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RpnPrinterTest {
    private static final Span S = new Span(0, 0, 1, 1);

    private static Token op(TokenType t, String lexeme) { return new Token(t, lexeme, null, S); }
    private static Expr num(double d) { return new Expr.Literal(d, S); }
    private static Expr group(Expr e) { return new Expr.Grouping(e, S); }

    @Test
    void bookExample() {   // (1 + 2) * (4 - 3)
        var expr = new Expr.Binary(
                group(new Expr.Binary(num(1), op(TokenType.PLUS, "+"), num(2), S)),
                op(TokenType.STAR, "*"),
                group(new Expr.Binary(num(4), op(TokenType.MINUS, "-"), num(3), S)),
                S);
        assertThat(RpnPrinter.print(expr)).isEqualTo("1.0 2.0 + 4.0 3.0 - *");
    }

    @Test
    void unaryMinusIsDistinctFromSubtraction() {   // -123 * 2
        var expr = new Expr.Binary(
                new Expr.Unary(op(TokenType.MINUS, "-"), num(123), S),
                op(TokenType.STAR, "*"),
                num(2),
                S);
        assertThat(RpnPrinter.print(expr)).isEqualTo("123.0 ~ 2.0 *");
    }

    @Test
    void doubleUnaryMinus() {   // --1
        var expr = new Expr.Unary(op(TokenType.MINUS, "-"),
                new Expr.Unary(op(TokenType.MINUS, "-"), num(1), S), S);
        assertThat(RpnPrinter.print(expr)).isEqualTo("1.0 ~ ~");
    }

    @Test
    void bangUnaryKeepsItsLexeme() {
        var expr = new Expr.Unary(op(TokenType.BANG, "!"), new Expr.Literal(true, S), S);
        assertThat(RpnPrinter.print(expr)).isEqualTo("true !");
    }

    @Test
    void nestedGroupingVanishes() {   // ((1))
        assertThat(RpnPrinter.print(group(group(num(1))))).isEqualTo("1.0");
    }

    @Test
    void literals() {
        assertThat(RpnPrinter.print(new Expr.Literal(null, S))).isEqualTo("nil");
        assertThat(RpnPrinter.print(new Expr.Literal(false, S))).isEqualTo("false");
        assertThat(RpnPrinter.print(new Expr.Literal("a b", S))).isEqualTo("\"a b\"");
        assertThat(RpnPrinter.print(new Expr.Literal("nil", S))).isEqualTo("\"nil\"");
    }
}
