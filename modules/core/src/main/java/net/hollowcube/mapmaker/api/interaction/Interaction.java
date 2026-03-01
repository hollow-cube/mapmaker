package net.hollowcube.mapmaker.api.interaction;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@RuntimeGson
public record Interaction(
    String id,
    Type type,
    String playerId,
    @UnknownNullability CommandData command
) {

    public enum Type {
        COMMAND
    }

    @RuntimeGson
    public record CommandData(List<CommandArgument> arguments) {

    }

    @RuntimeGson
    public record CommandArgument(
        String name,
        Command.Argument.Type type,
        Object value
    ) {

    }

}
