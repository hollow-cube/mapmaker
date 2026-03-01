package net.hollowcube.mapmaker.api.interaction;

import com.google.gson.annotations.JsonAdapter;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.util.gson.UnsignedLongAdapter;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@RuntimeGson
public record RemoteInteraction(
    Type type,
    String name,
    @MagicConstant(flagsFromClass = Permission.class)
    @JsonAdapter(UnsignedLongAdapter.class)
    long permissions,

    List<Argument> arguments
) {

    public enum Type {
        COMMAND
    }

    @RuntimeGson
    public record Argument(
        Type type,
        String name,

        @Nullable List<String> choices
    ) {
        public enum Type {
            WORD,
            STRING,
            CHOICE,
            PLAYER,
            DYNAMIC
        }
    }

}
