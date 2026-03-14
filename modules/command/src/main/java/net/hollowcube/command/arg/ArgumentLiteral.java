package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;

import java.util.Locale;
import java.util.Objects;

public class ArgumentLiteral extends Argument<String> {
    private final String literal;

    public ArgumentLiteral(String literal) {
        super(literal);
        this.literal = literal;
    }

    @Override
    public ParseResult<String> parse(CommandSender sender, StringReader reader) {
        var compareValue = literal.toLowerCase(Locale.ROOT);

        int pos = reader.pos();
        var word = reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT);
        if (compareValue.equals(word)) return success(literal);
        else if (compareValue.startsWith(word)) return partial();
        else return syntaxError(pos);
    }

    @Override
    public void suggest(CommandSender sender, String raw, Suggestion suggestion) {
        if (literal.toLowerCase(Locale.ROOT).startsWith(raw.toLowerCase(Locale.ROOT))) suggestion.add(literal);
    }

    @Override
    public String toString() {
        return "literal@" + id();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgumentLiteral that = (ArgumentLiteral) o;
        return Objects.equals(literal, that.literal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(literal);
    }

    @Override
    public DeclareCommandsPacket.NodeType getType() {
        return DeclareCommandsPacket.NodeType.LITERAL;
    }
}
