package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.lexer.TokenType;

/**
 * Prints an expression in Reverse Polish Notation (ch. 5, challenge 3), e.g.
 * {@code (1 + 2) * (4 - 3)} becomes {@code 1.0 2.0 + 4.0 3.0 - *}.
 *
 * <p>Grouping prints nothing: tree shape already fixes evaluation order. Unary minus prints as
 * {@code ~} because in postfix {@code -} already means binary subtraction, so {@code 1 2 -}
 * (subtract) and {@code 1 ~} (negate) must differ. String literals are printed in double quotes.
 * Recursive; very deep trees overflow the stack.
 */
public final class RpnPrinter {
    private RpnPrinter() {}

    public static String print(Expr expr) {
        return switch (expr) {
            case Expr.Binary b   -> print(b.left()) + " " + print(b.right()) + " " + b.operator().lexeme();
            case Expr.Grouping g -> print(g.expression());
            case Expr.Literal l  -> literal(l.value());
            case Expr.Unary u    -> print(u.right()) + " "
                + (u.operator().type() == TokenType.MINUS ? "~" : u.operator().lexeme());
        };
    }

    private static String literal(Object value) {
        return switch (value) {
            case null     -> "nil";
            case String s -> "\"" + s + "\"";
            default       -> value.toString();
        };
    }
}
