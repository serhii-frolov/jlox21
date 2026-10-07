package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.Span;
import io.github.serhiifrolov.jlox.lexer.Token;
import io.github.serhiifrolov.jlox.lexer.TokenType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AstPrinterTest {
    private static final Span S = new Span(0, 0, 1, 1);
    private static Token op(TokenType t, String lexeme) { return new Token(t, lexeme, null, S); }

    @Test
    void bookExample() {
        var expr = new Expr.Binary(
                new Expr.Unary(op(TokenType.MINUS, "-"), new Expr.Literal(123.0, S), S),
                op(TokenType.STAR, "*"),
                new Expr.Grouping(new Expr.Literal(45.67, S), S),
                S
        );
        assertEquals("(* (- 123.0) (group 45.67))", AstPrinter.print(expr));
    }
    @Test void nilLiteral(){
        assertEquals("nil", AstPrinter.print(new Expr.Literal(null, S)));
    }
    @Test void stringLiteral(){
        assertEquals("hi", AstPrinter.print(new Expr.Literal("hi", S)));
    }
    @Test void booleanLiteral(){
        assertEquals("true", AstPrinter.print(new Expr.Literal(true, S)));
    }
    @Test void nestedBinary() {
        var expr = new Expr.Binary(
                new Expr.Literal(1.0, S),
                op(TokenType.PLUS, "+"),
                new Expr.Binary(
                        new Expr.Literal(2.0, S),
                        op(TokenType.STAR, "*"),
                        new Expr.Literal(3.0, S),
                        S
                ),
                S
        );
        assertEquals("(+ 1.0 (* 2.0 3.0))", AstPrinter.print(expr));
    }
}
