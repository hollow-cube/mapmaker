package net.hollowcube.terraform.mask.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Lexer {
    private final String source;

    private int start = 0;
    private int cursor = 0;

    public Lexer(@NotNull String source) {
        this.source = source;
    }

    public int pos() {
        return start;
    }

    public @Nullable Token next() {
        start = cursor;

        // Identifier chars are a-z, A-Z, 0-9, -, _

        if (atEnd()) return null;
        consumeWhitespace();

        char c = advance();
        if (isDigit(c))
            return number();
        if (isIdent(c))
            return ident();

        return symbol(c);
    }

    public @Nullable Token peek() {
        var result = next();
        cursor = start; // Reset to where it was before the call to next.
        return result;
    }

    public @NotNull String span(@NotNull Token token) {
        return source.substring(token.start(), token.end()).strip();
    }

    // Impl

    private void consumeWhitespace() {
        while (true) {
            switch (peek0()) {
                case ' ', '\t', '\r', '\n' -> advance();
                default -> {
                    return;
                }
            }
        }
    }

    private Token ident() {
        while (isIdent(peek0())) {
            advance();
        }

        return new Token(Token.Type.IDENT, start, cursor);
    }

    private Token number() {
        // Pre decimal
        while (isDigit(peek0()))
            advance();

        // Decimal, if present
        if (match('.')) {
            while (isDigit(peek0()))
                advance();
        }

        return new Token(Token.Type.NUMBER, start, cursor);
    }

    private Token symbol(char c) {
        return switch (c) {
            case '!' -> new Token(Token.Type.BANG, start, cursor);
            case '%' -> new Token(Token.Type.PERCENT, start, cursor);
            case '#' -> new Token(Token.Type.HASH, start, cursor);
            case ',' -> new Token(Token.Type.COMMA, start, cursor);
            case '|' -> new Token(Token.Type.PIPE, start, cursor);
            case '&' -> new Token(Token.Type.AMP, start, cursor);
            case '=' -> new Token(Token.Type.EQUALS, start, cursor);
            case '(' -> new Token(Token.Type.LPAREN, start, cursor);
            case ')' -> new Token(Token.Type.RPAREN, start, cursor);
            case '[' -> new Token(Token.Type.LBRACKET, start, cursor);
            case ']' -> new Token(Token.Type.RBRACKET, start, cursor);
            default -> throw new RuntimeException("Unexpected symbol: " + c);
        };
    }

    // Source movement

    private boolean atEnd() {
        return cursor >= source.length();
    }

    private char peek0() {
        if (atEnd())
            return '\u0000';
        return source.charAt(cursor);
    }

    private char advance() {
        if (atEnd()) throw new RuntimeException("Unexpected end of input");
        return source.charAt(cursor++);
    }

    private boolean match(char c) {
        if (atEnd()) return false;
        if (peek0() != c) return false;
        advance();
        return true;
    }

    private boolean isIdent(char c) {
        return isDigit(c) || isAlpha(c) || c == ':' || c == '_';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
