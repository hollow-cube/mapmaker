package net.hollowcube.terraform.mask.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class Parser {
    private final Lexer lexer;

    public Parser(@NotNull String source) {
        this.lexer = new Lexer(source);
    }

    public Tree parse() throws MaskParseException {
        return root(0);
    }

    private @Nullable Tree root(int mbp) throws MaskParseException {
        Tree lhs = lhs();
        if (lhs == null) {
            // End of input
            return null;
        }

        while (true) {
            var token = lexer.peek();
            if (token == null) break;

            var bp = getBindingPower(token.type());
            if (bp == null) {
                throw new RuntimeException("todo what do i do here");
            }
            if (bp.left < mbp) break;
            lexer.next();

            // Parse the right side expression
            var rhs = root(bp.right);
            lhs = new Tree.Infix(lhs.start(), rhs == null ? token.end() : rhs.end(), bp.type, lhs, rhs);
        }

        return lhs;
    }

    private @Nullable Tree lhs() throws MaskParseException {
        var token = lexer.peek();
        if (token == null)
            return null;
        return switch (token.type()) {
            case IDENT -> blockState();
            case BANG -> not();
            case PERCENT -> uniformNoise();
            //todo should not be here? How would we give completions based on this
            //todo probably add some kind of unknown token to the tree and then its an error later.
            default -> throw new MaskParseException(token.start(), token.end(),
                    "Unexpected token " + token.type());
        };
    }

    private @NotNull Tree.BlockState blockState() {
        var ident = Objects.requireNonNull(lexer.next()); // First identifier, eg "stone"
        int start = ident.start(), end = ident.end();
        var props = new ArrayList<Tree.BlockState.Property>();

        // Check for properties
        int openBracket = -1, closeBracket = -1;
        var openBracketToken = lexer.peek();
        if (openBracketToken != null && openBracketToken.type() == Token.Type.LBRACKET) {
            lexer.next();
            openBracket = openBracketToken.start();
            end = openBracketToken.end();

            var tok = lexer.peek();
            while (tok != null && tok.type() == Token.Type.IDENT) {
                String name = lexer.span(tok), value = null;
                int propStart = tok.start(), propEnd = tok.end();
                lexer.next();

                tok = lexer.peek();
                if (tok != null && tok.type() == Token.Type.EQUALS) {
                    end = propEnd = tok.end();
                    lexer.next();

                    tok = lexer.peek();
                    if (tok != null && tok.type() == Token.Type.IDENT) {
                        value = lexer.span(tok);
                        propEnd = tok.end();
                        lexer.next();

                        tok = lexer.peek();
                    }
                }

                props.add(new Tree.BlockState.Property(propStart, propEnd, name, value));

                // If there is a comma following, eat that and continue
                if (tok != null && tok.type() == Token.Type.COMMA) {
                    end = tok.end();
                    lexer.next();
                    tok = lexer.peek();
                }
            }

            // If the next token is an rbracket, record it. Otherwise, we move on
            // and leave it as -1. When converting to a mask, this will be checked.
            // We must not error when parsing, because we still need to give completions.
            if (tok != null && tok.type() == Token.Type.RBRACKET) {
                lexer.next();
                closeBracket = tok.start();
                end = tok.end();
            }
        }

        return new Tree.BlockState(start, end, openBracket, closeBracket,
                lexer.span(ident), props.isEmpty() ? null : props);
    }

    private @NotNull Tree not() throws MaskParseException {
        var bang = Objects.requireNonNull(lexer.next());
        int start = bang.start(), end = bang.end();

        var child = lhs();
        if (child != null) {
            end = child.end();
        }

        return new Tree.Prefix(start, end, Tree.Prefix.Type.NOT, child);
    }

    private @NotNull Tree uniformNoise() {
        var percent = Objects.requireNonNull(lexer.next());
        int start = percent.start(), end = percent.end();

        var child = number();
        if (child != null) {
            end = child.end();
        }

        return new Tree.Prefix(start, end, Tree.Prefix.Type.UNIFORM_NOISE, child);
    }

    private @Nullable Tree number() {
        var number = lexer.peek();
        if (number == null || number.type() != Token.Type.NUMBER)
            return null;

        lexer.next();
        return new Tree.Number(number.start(), number.end(),
                Double.parseDouble(lexer.span(number)));
    }


    private record InfixOp(int left, int right, Tree.Infix.Type type) {}

    private @Nullable InfixOp getBindingPower(@NotNull Token.Type type) {
        return switch (type) {
            case PIPE -> new InfixOp(12, 13, Tree.Infix.Type.OR);
            case AMP -> new InfixOp(3, 3, Tree.Infix.Type.AND);
            default -> null;
        };
    }
}
