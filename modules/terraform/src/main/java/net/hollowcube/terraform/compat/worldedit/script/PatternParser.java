package net.hollowcube.terraform.compat.worldedit.script;

import net.hollowcube.terraform.util.script.Lexer;
import net.hollowcube.terraform.util.script.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class PatternParser {
    private final Lexer lexer;

    public PatternParser(@NotNull String source) {
        this.lexer = new Lexer(source);
    }

    public PatternTree parse() {
        return list();
    }

    /**
     * Parses a list of patterns with optional weights, ie `stone,dirt,grass`, `5%stone,50%grass,10%dirt`.
     *
     * @return the parsed pattern tree
     */
    private @Nullable PatternTree list() {
        var entries = new ArrayList<PatternTree>();
        while (true) {
            var entry = single(true);
            if (entry == null)
                break;
            entries.add(entry);

            var tok = lexer.peek();
            if (tok == null || tok.type() != Token.Type.COMMA)
                break;
            lexer.next(); // Eat the comma
        }

        if (entries.isEmpty())
            return null;
        if (entries.size() == 1)
            return entries.get(0);
        return new PatternTree.WeightedList(
                entries.get(0).start(),
                entries.get(entries.size() - 1).end(),
                entries
        );
    }

    private @Nullable PatternTree single(boolean topLevel) {
        var tok = lexer.peek();
        if (tok == null)
            return null;
        return switch (tok.type()) {
            case NUMBER -> //noinspection DataFlowIssue
                    topLevel ? weightOrBlockId() : legacyBlock(lexer.next());
            case IDENT -> blockState();
            case STAR -> randomState();
            default -> {
                lexer.next(); // Eat the bad symbol
                yield new PatternTree.Error(tok.start(), tok.end());
            }
        };
    }

    private @NotNull PatternTree randomState() {
        var starTok = Objects.requireNonNull(lexer.next());
        PatternTree.NamespaceId namespaceId = null;

        var tok = lexer.peek();
        if (tok != null && tok.type() == Token.Type.IDENT) {
            namespaceId = namespaceId();
        }

        return new PatternTree.RandomState(starTok.start(), namespaceId == null ? starTok.end() : namespaceId.end(), namespaceId);
    }

//    private @NotNull PatternTree randomState() {
//        var starTok = Objects.requireNonNull(lexer.next());
//        PatternTree.NamespaceId namespaceId = null;
//
//        var tok = lexer.peek();
//        if (tok != null && tok.type() == Token.Type.IDENT) {
//            namespaceId = namespaceId();
//        }
//
//        return new PatternTree.RandomState(starTok.start(), namespaceId == null ? starTok.end() : namespaceId.end(), namespaceId);
//    }

    private @NotNull PatternTree weightOrBlockId() {
        var numTok = Objects.requireNonNull(lexer.next());
        var next = lexer.peek();
        return next != null && next.type() == Token.Type.PERCENT
                ? weighted(numTok) : legacyBlock(numTok);
    }

    private @NotNull PatternTree weighted(@NotNull Token numTok) {
        var weight = Integer.parseInt(lexer.span(numTok));
        lexer.next(); // Eat the percent

        var inner = single(false);
        return new PatternTree.Weighted(
                numTok.start(), inner == null ? lexer.pos() : inner.end(),
                weight, inner
        );
    }

    private @NotNull PatternTree legacyBlock(@NotNull Token numTok) {
        var blockId = Integer.parseInt(lexer.span(numTok));
        int end = numTok.end(), colon = -1, blockData = -1;

        var tok = lexer.peek();
        if (tok != null && tok.type() == Token.Type.COLON) {
            colon = tok.start();
            lexer.next(); // Eat the colon
            end = tok.end();

            tok = lexer.peek();
            if (tok != null && tok.type() == Token.Type.NUMBER) {
                blockData = Integer.parseInt(lexer.span(tok));
                lexer.next(); // Eat the number
                end = tok.end();
            }
        }

        return new PatternTree.LegacyBlock(
                numTok.start(), end, colon, blockId, blockData
        );
    }

    private @NotNull PatternTree blockState() {
        return new PatternTree.BlockState(namespaceId(), propertyList());
    }

    private @NotNull PatternTree.NamespaceId namespaceId() {
        var ident = Objects.requireNonNull(lexer.next());
        int start = ident.start(), end = ident.end();
        int colon = -1;
        var tok = lexer.peek();
        if (tok != null && tok.type() == Token.Type.COLON) {
            colon = tok.start();
            end = tok.end();
            lexer.next();
            tok = lexer.peek();
            if (tok != null && tok.type() == Token.Type.IDENT) {
                end = tok.end();
                lexer.next();
            }
        }
        return new PatternTree.NamespaceId(start, end, colon, lexer.span(ident),
                tok == null ? null : lexer.span(tok));
    }

    /**
     * Parses a block state-style property list. If there is not a [ in the next spot
     * then an empty list will be returned.
     */
    private @NotNull PatternTree.PropertyList propertyList() {
        int start = lexer.pos(), end = start;
        int openBracket = -1, closeBracket = -1;
        var props = new ArrayList<PatternTree.PropertyList.Property>();

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

                props.add(new PatternTree.PropertyList.Property(propStart, propEnd, name, value));

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

        return new PatternTree.PropertyList(start, end, openBracket, closeBracket, props);
    }
}
