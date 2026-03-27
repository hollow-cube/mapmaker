package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record PunishmentUpdateMessage(@NotNull Action action, @NotNull Punishment punishment, @NotNull String message) {

    public Component component() {
        return MiniMessage.miniMessage().deserialize(this.message);
    }

    // Serialized as ordinal
    public enum Action {
        CREATE,
        REVOKE
    }
}
