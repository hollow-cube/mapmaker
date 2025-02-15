package net.hollowcube.terraform.compat.worldedit.script;

import net.hollowcube.terraform.util.script.Lexer;
import net.hollowcube.terraform.util.script.Token;
import net.minestom.server.entity.PlayerHand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class PatternParser {
    private final Lexer lexer;

    public PatternParser(@NotNull String source) {
        this.lexer = new Lexer(source);
    }

    public @Nullable PatternTree parse() {
        return list();
    }

    /**
     * Parses a list of patterns with optional weights, ie `stone,dirt,grass`, `5%stone,50%grass,10%dirt`.
     *
     * @return the parsed pattern tree
     */
    private @Nullable PatternTree list() {
        int trailingComma = -1;
        var entries = new ArrayList<PatternTree>();
        while (true) {
            var entry = single(true);
            if (entry == null)
                break;
            entries.add(entry);
            trailingComma = -1;

            var tok = lexer.peek();
            if (tok == null || tok.type() != Token.Type.COMMA)
                break;
            trailingComma = tok.start();
            lexer.next(); // Eat the comma
        }

        if (entries.isEmpty())
            return null;
        // If there is only one unweighted entry, return it alone.
        if (trailingComma == -1 && entries.size() == 1 && !(entries.getFirst() instanceof PatternTree.Weighted))
            return entries.getFirst();
        return new PatternTree.WeightedList(trailingComma, entries);
    }

    private @Nullable PatternTree single(boolean topLevel) {
        var tok = lexer.peek();
        if (tok == null)
            return null;
        return switch (tok.type()) {
            case NUMBER -> //noinspection DataFlowIssue
                    topLevel ? weightOrBlockId() : legacyBlock(lexer.next());
            case IDENT -> identifier(lexer.span(tok));
            case STAR -> randomState();
            case HASH -> tagOrNamed();
            case CARET -> typeStateApply();
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

    private @NotNull PatternTree tagOrNamed() {
        var firstHash = Objects.requireNonNull(lexer.next());
        var next = lexer.peek();
        // There is a subtle detail here that if we reach end of input, we return a 'named'
        // which means suggestions for that will be returned, not for tags.
        // If this behavior needs to change to suggest both, most likely we will need some TagOrNamed tree value.
        if (next != null && next.type() == Token.Type.HASH) {
            return tag(firstHash);
        } else {
            return named(firstHash);
        }
    }

    private @NotNull PatternTree tag(@NotNull Token firstHashTok) {
        var secondHashTok = Objects.requireNonNull(lexer.next());
        int start = firstHashTok.start(), end = secondHashTok.end();

        // Consume the * if it exists
        int star = -1;
        var tok = lexer.peek();
        if (tok != null && tok.type() == Token.Type.STAR) {
            star = tok.start();
            lexer.next();
            end = tok.end();
        }

        // Read the namespaceId following
        PatternTree.NamespaceId namespaceId = null;
        if ((tok = lexer.peek()) != null && tok.type() == Token.Type.IDENT) {
            namespaceId = namespaceId();
            end = namespaceId.end();
        }

        return new PatternTree.Tag(start, end, star, namespaceId);
    }

    private @NotNull PatternTree typeStateApply() {
        var tok = Objects.requireNonNull(lexer.next());
        int start = tok.start(), end = tok.end();


        PatternTree.NamespaceId namespaceId = null;
        if ((tok = lexer.peek()) != null && tok.type() == Token.Type.IDENT) {
            namespaceId = namespaceId();
            end = namespaceId.end();
        }

        PatternTree.PropertyList propertyList = null;
        if ((tok = lexer.peek()) != null && tok.type() == Token.Type.LBRACKET) {
            propertyList = propertyList();
            end = propertyList.end();
        }

        return new PatternTree.TypeStateApply(start, end, namespaceId, propertyList);
    }

    private @NotNull PatternTree named(@NotNull Token firstHashTok) {
        //todo named args should be implemented with command args. Basically any Argument<> should be a valid argument to one of these.
        // it means suggestions are automatically valid and things like patterns and masks are immediately valid.
        throw new UnsupportedOperationException("not implemented");
    }

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

    private PatternTree identifier(String identifier) {
        return switch (identifier) {
            case "hand", "h", "offhand", "oh" -> {
                var token = Objects.requireNonNull(lexer.next());
                var hand = identifier.startsWith("o") ? PlayerHand.OFF : PlayerHand.MAIN;
                yield new PatternTree.Hand(token.start(), token.end(), hand);
            }
            default -> new PatternTree.BlockState(namespaceId(), propertyList());
        };
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
            } else tok = null;
        } else tok = null;
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
        int trailingComma = -1;

        var openBracketToken = lexer.peek();
        if (openBracketToken != null && openBracketToken.type() == Token.Type.LBRACKET) {
            lexer.next();
            openBracket = openBracketToken.start();
            end = openBracketToken.end();

            var tok = lexer.peek();
            while (tok != null && tok.type() == Token.Type.IDENT) {
                String name = lexer.span(tok), value = null;
                int propStart = tok.start(), propEnd = tok.end();
                end = propEnd;
                lexer.next();
                trailingComma = -1;

                int equals = -1;
                tok = lexer.peek();
                if (tok != null && tok.type() == Token.Type.EQUALS) {
                    end = propEnd = tok.end();
                    equals = tok.start();
                    lexer.next();

                    tok = lexer.peek();
                    if (tok != null && (tok.type() == Token.Type.IDENT || tok.type() == Token.Type.NUMBER)) {
                        value = lexer.span(tok);
                        propEnd = tok.end();
                        lexer.next();

                        tok = lexer.peek();
                    }
                }

                end = propEnd;
                props.add(new PatternTree.PropertyList.Property(propStart, propEnd, equals, name, value));

                // If there is a comma following, eat that and continue
                if (tok != null && tok.type() == Token.Type.COMMA) {
                    end = tok.end();
                    lexer.next();
                    trailingComma = tok.start();
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

        return new PatternTree.PropertyList(start, end, openBracket, closeBracket, trailingComma, props);
    }
}
