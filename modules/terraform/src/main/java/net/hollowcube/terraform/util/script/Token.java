package net.hollowcube.terraform.util.script;

import org.jetbrains.annotations.NotNull;

public record Token(@NotNull Type type, int start, int end) {
    public enum Type {
        IDENT, // Any identifier, ex "stone", "minecraft:stone"
        NUMBER, // Any number, ex 1, 1.0, 1.0

        BANG,       // !
        PERCENT,    // %
        COLON,      // :
        HASH,       // #
        COMMA,      // ,
        PIPE,       // |
        AMP,        // &
        EQUALS,     // =
        STAR,       // *
        LPAREN,     // (
        RPAREN,     // )
        LBRACKET,   // [
        RBRACKET,   // ]
    }
}
