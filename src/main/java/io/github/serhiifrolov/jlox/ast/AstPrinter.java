package io.github.serhiifrolov.jlox.ast;

/**
 * Prints an expression as a Lisp-style S-expression, e.g. {@code (* (- 123.0) (group 45.67))}.
 *
 * <p>Debug/test output, not user-facing. String literals are printed in double quotes so
 * {@code "nil"} and {@code nil} stay distinguishable. Recursive; very deep trees overflow the stack.
 */
public final class AstPrinter {
    private AstPrinter() {}

    public static String print(Expr expr) {
        return switch (expr) {
            case Expr.Binary b   -> parenthesize(b.operator().lexeme(), b.left(), b.right());
            case Expr.Grouping g -> parenthesize("group", g.expression());
            case Expr.Literal l  -> literal(l.value());
            case Expr.Unary u    -> parenthesize(u.operator().lexeme(), u.right());
        };
    }

    private static String parenthesize(String name, Expr... exprs) {
        var sb = new StringBuilder("(").append(name);
        for (Expr e : exprs) {
            sb.append(' ').append(print(e));
        }
        return sb.append(')').toString();
    }

    private static String literal(Object value) {
        return switch (value) {
            case null     -> "nil";
            case String s -> "\"" + s + "\"";
            default       -> value.toString();
        };
    }
}
