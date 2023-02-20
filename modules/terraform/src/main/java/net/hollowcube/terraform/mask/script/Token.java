package net.hollowcube.terraform.mask.script;

import org.jetbrains.annotations.NotNull;

public record Token(@NotNull Type type, int start, int end) {
    public enum Type {
        IDENT, // Any identifier, ex "stone", "minecraft:stone"
        NUMBER, // Any number, ex 1, 1.0, 1.0

        BANG,       // !
        PERCENT,    // %
        HASH,       // #
        COMMA,      // ,
        PIPE,       // |
        AMP,        // &
        EQUALS,     // =
        LPAREN,     // (
        RPAREN,     // )
        LBRACKET,   // [
        RBRACKET,   // ]
    }
}
