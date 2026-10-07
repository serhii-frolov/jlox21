package io.github.serhiifrolov.jlox.ast;

import io.github.serhiifrolov.jlox.lexer.TokenType;

/** Prints an expression in Reverse Polish Notation (ch. 5, challenge 3). */
public final class RpnPrinter {
    private RpnPrinter() {}

    public static String print(Expr expr) {
        return switch (expr) {
            case Expr.Binary b   -> print(b.left()) + " " + print(b.right()) + " " + b.operator().lexeme();
            case Expr.Grouping g -> print(g.expression());
            case Expr.Literal l  -> l.value() == null ? "nil" : l.value().toString();
            // Unary minus prints as "~": in RPN "-" already means subtraction.
            case Expr.Unary u    -> print(u.right()) + " "
                + (u.operator().type() == TokenType.MINUS ? "~" : u.operator().lexeme());
        };
    }
}
