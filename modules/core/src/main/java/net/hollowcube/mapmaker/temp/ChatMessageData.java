package net.hollowcube.mapmaker.temp;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

@RuntimeGson
public record ChatMessageData(
    ClientChatMessageData.Type type,

    // Unsigned chat
    String sender,
    String channel,
    List<Part> parts,
    long seed,
    boolean senderHasHypercube,

    // System message
    String target,
    String key,
    @Nullable List<String> args,
    boolean respectClientSettings,

    @Nullable ChatMessageData extra
) {

    @RuntimeGson
    public record Part(
        Type type,

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

    public List<String> argsSafe() {
        return args == null ? List.of() : args;
    }

}
