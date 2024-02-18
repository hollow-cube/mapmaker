package net.hollowcube.mapmaker.temp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public record ChatMessageData(
        @NotNull ClientChatMessageData.Type type,

        @NotNull String sender,
        @NotNull String channel,
        @NotNull List<Part> parts,

        @NotNull String target,
        @NotNull String key,
        @Nullable List<String> args
) {

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
