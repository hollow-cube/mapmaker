package net.hollowcube.mapmaker.temp;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@RuntimeGson
public record ChatMessageData(
        @NotNull ClientChatMessageData.Type type,

        // Unsigned chat
        @NotNull String sender,
        @NotNull String channel,
        @NotNull List<Part> parts,
        long seed,
        boolean senderHasHypercube,

        // System message
        @NotNull String target,
        @NotNull String key,
        @Nullable List<String> args,

        @Nullable ChatMessageData extra
) {

    @RuntimeGson
    public record Part(
            @NotNull Type type,

            // RAW
            @UnknownNullability String text,

            // EMOJI
            @UnknownNullability String name,

            // MAP
            @UnknownNullability String mapId
    ) {

        public enum Type {
            RAW, EMOJI, MAP, URL
        }

    }

    public @NotNull List<String> argsSafe() {
        return args == null ? List.of() : args;
    }

}
