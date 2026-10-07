package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;
import io.github.serhiifrolov.jlox.lexer.TokenType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AstPrinterTest {
    private static final Span S = new Span(0, 0, 1, 1);

    private static Token op(TokenType t, String lexeme) { return new Token(t, lexeme, null, S); }
    private static Expr num(double d) { return new Expr.Literal(d, S); }

    @Test
    void bookExample() {   // -123 * (45.67)
        var expr = new Expr.Binary(
                new Expr.Unary(op(TokenType.MINUS, "-"), num(123), S),
                op(TokenType.STAR, "*"),
                new Expr.Grouping(num(45.67), S),
                S);
        assertThat(AstPrinter.print(expr)).isEqualTo("(* (- 123.0) (group 45.67))");
    }

    @Test
    void nestedBinary() {   // 1 + 2 * 3
        var expr = new Expr.Binary(num(1), op(TokenType.PLUS, "+"),
                new Expr.Binary(num(2), op(TokenType.STAR, "*"), num(3), S), S);
        assertThat(AstPrinter.print(expr)).isEqualTo("(+ 1.0 (* 2.0 3.0))");
    }

    @Test
    void nilLiteral() {
        assertThat(AstPrinter.print(new Expr.Literal(null, S))).isEqualTo("nil");
    }

    @Test
    void booleanLiteral() {
        assertThat(AstPrinter.print(new Expr.Literal(true, S))).isEqualTo("true");
    }

    @Test
    void stringLiteralIsQuoted() {
        assertThat(AstPrinter.print(new Expr.Literal("hi", S))).isEqualTo("\"hi\"");
    }

    @Test
    void stringLiteralNilIsDistinctFromNil() {
        assertThat(AstPrinter.print(new Expr.Literal("nil", S))).isEqualTo("\"nil\"");
    }

    @Test
    void stringLiteralWithSpaces() {   // "a b" + 1
        var expr = new Expr.Binary(new Expr.Literal("a b", S), op(TokenType.PLUS, "+"), num(1), S);
        assertThat(AstPrinter.print(expr)).isEqualTo("(+ \"a b\" 1.0)");
    }

    @Test
    void bangUnary() {
        var expr = new Expr.Unary(op(TokenType.BANG, "!"), new Expr.Literal(true, S), S);
        assertThat(AstPrinter.print(expr)).isEqualTo("(! true)");
    }

    @Test
    void doubleUnaryMinus() {   // --1
        var expr = new Expr.Unary(op(TokenType.MINUS, "-"),
                new Expr.Unary(op(TokenType.MINUS, "-"), num(1), S), S);
        assertThat(AstPrinter.print(expr)).isEqualTo("(- (- 1.0))");
    }

    @Test
    void doubleFormattingFollowsJava() {
        assertThat(AstPrinter.print(num(1e10))).isEqualTo("1.0E10");
        assertThat(AstPrinter.print(num(-0.0))).isEqualTo("-0.0");
    }
}
