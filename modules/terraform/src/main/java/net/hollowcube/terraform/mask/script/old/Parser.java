package net.hollowcube.terraform.mask.script.old;

import net.hollowcube.terraform.mask.script.Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Parser {
    private final Lexer lexer;

    public Parser(@NotNull String source) {
        this.lexer = new Lexer(source);
    }

    public Expr parse() {
        return expr(0);
    }

    private @Nullable Expr expr(int minBindingPower) {
        Expr lhs = lhs();
        if (lhs == null) {
            // End of input
            return null;
        }

        while (true) {
            var token = lexer.peek();
            if (token == null) break;

            var lbp = switch (token.type()) {
                case PIPE -> 12;
                default -> throw new RuntimeException("Unexpected token: " + token);
            };
            if (lbp < minBindingPower) break;
            lexer.next();

            // Parse the right side expression
            var rhs = expr(13);
            lhs = new Expr.Binary(lhs, rhs);
        }

        return lhs;
    }

    private @Nullable Expr lhs() {
        var token = lexer.peek();
        if (token == null)
            return new Expr.PlaceholderRoot(lexer.pos(), lexer.pos());
        return switch (token.type()) {
            case IDENT -> blockState();
            case BANG -> not();
            default -> throw new RuntimeException("Unexpected token: " + token);
        };
    }

    private @NotNull Expr blockState() {
        var ident = lexer.next(); // First identifier, eg "stone"
        return new Expr.BlockState(ident.start(), ident.end(), lexer.span(ident));
    }

    private @NotNull Expr not() {
        var bang = lexer.next();
        var expr = expr(10);
        return new Expr.Not(bang.start(), expr);
    }
}
