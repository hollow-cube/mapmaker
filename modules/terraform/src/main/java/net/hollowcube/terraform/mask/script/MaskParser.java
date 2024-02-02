package net.hollowcube.terraform.mask.script;

import net.hollowcube.terraform.util.script.Lexer;
import net.hollowcube.terraform.util.script.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class MaskParser {
    private final Lexer lexer;

    public MaskParser(@NotNull String source) {
        this.lexer = new Lexer(source);
    }

    public Tree parse() {
        return root(0);
    }

    private @Nullable Tree root(int mbp) {
        Tree lhs = lhs();
        if (lhs == null) {
            // End of input
            return null;
        }

        while (true) {
            var token = lexer.peek();
            if (token == null) break;

            // Get binding power for the next token, or an error operator if it is not valid
            var bp = getBindingPower(token.type());
            if (bp == null) break;
            if (bp.left < mbp) break;
            lexer.next();

            // Parse the right side expression
            var rhs = root(bp.right);
            lhs = new Tree.Infix(lhs.start(), rhs == null ? token.end() : rhs.end(), bp.type, lhs, rhs);
        }

        return lhs;
    }

    private @Nullable Tree lhs() {
        var token = lexer.peek();
        if (token == null)
            return null;
        return switch (token.type()) {
            case IDENT -> blockState();
            case BANG -> not();
            case PERCENT -> uniformNoise();
            case HASH -> named();
            default -> {
                lexer.next(); // Eat the bad symbol
                yield new Tree.Error(token.start(), token.end());
            }
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

    private @NotNull Tree not() {
        var bang = Objects.requireNonNull(lexer.next());
        int start = bang.start(), end = bang.end();

        var child = lhs(); //todo i think this should be a call back to root with the BP of not
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

    private @NotNull Tree named() {
        var hash = Objects.requireNonNull(lexer.next());
        int start = hash.start(), end = hash.end();

        // Parse name
        var name = lexer.peek();
        if (name == null || name.type() != Token.Type.IDENT)
            return new Tree.Named(start, end, -1, -1, null, null);
        lexer.next();
        end = name.end();

        // Open bracket
        int openBracket = -1, closeBracket = -1;
        var openBracketToken = lexer.peek();
        if (openBracketToken == null || openBracketToken.type() != Token.Type.LBRACKET)
            return new Tree.Named(start, end, -1, -1, lexer.span(name), null);
        lexer.next();
        openBracket = openBracketToken.start();
        end = openBracketToken.end();

        var args = new ArrayList<Tree.Named.Argument>();
        var tok = lexer.peek();
        while (tok != null && tok.type() != Token.Type.RBRACKET) {
            int argStart = tok.start(), argEq = -1, argEnd = tok.end();

            String argName = null;
            Tree argValue = null;

            // If there is an identifier following, it is a named argument (MAYBE)
            //todo this isnt really valid. A block state would have an ident following as well.
            // kinda need to look ahead by 1 to see if there is an equals sign
//                if (tok.type() == Token.Type.IDENT) {
//                    lexer.next();
//                    argName = lexer.span(tok);
//
//                    tok = lexer.peek();
//                    if (tok != null && tok.type() == Token.Type.EQUALS) {
//                        // Equals sign
//                        lexer.next();
//                        argEq = tok.start();
//                        argEnd = tok.end();
//                    }
//                }

            if (tok.type() == Token.Type.NUMBER) {
                // Number argument
                argValue = number();
            } else if (tok.type() != Token.Type.COMMA && tok.type() != Token.Type.RBRACKET) {
                // Attempt to parse as a mask
                argValue = root(0);
                if (argValue != null) {
                    argEnd = argValue.end();
                }
            }

            args.add(new Tree.Named.Argument(argStart, argEnd, argName, argEq, argValue));
            end = argEnd;

            // If there is a comma following, eat that and continue
            tok = lexer.peek();
            if (tok != null && tok.type() == Token.Type.COMMA) {
                end = tok.end();
                lexer.next();
                tok = lexer.peek();
            }
        }

        // Close bracket
        var closeBracketToken = tok;
        if (closeBracketToken == null || closeBracketToken.type() != Token.Type.RBRACKET)
            return new Tree.Named(start, end, openBracket, -1, lexer.span(name), args);
        lexer.next();
        closeBracket = closeBracketToken.start();
        end = closeBracketToken.end();

        return new Tree.Named(start, end, openBracket, closeBracket, lexer.span(name), args);
    }


    private record InfixOp(int left, int right, Tree.Infix.Type type) {
    }

    private @Nullable InfixOp getBindingPower(@NotNull Token.Type type) {
        return switch (type) {
            case PIPE -> new InfixOp(12, 13, Tree.Infix.Type.OR);
            case AMP -> new InfixOp(3, 3, Tree.Infix.Type.AND);
            case RBRACKET -> null; //todo this is a hack
            default -> new InfixOp(0, 0, Tree.Infix.Type.ERROR);
        };
    }
}
