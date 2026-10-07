package io.github.serhiifrolov.jlox.ast;

public class AstPrinter {
    public static String print(Expr expr) {
        return switch (expr) {
            case Expr.Binary b   -> parenthesize(b.operator().lexeme(), b.left(), b.right());
            case Expr.Grouping g -> parenthesize("group", g.expression());
            case Expr.Literal l  -> l.value() == null ? "nil" : l.value().toString();
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
}

