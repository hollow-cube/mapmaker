package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@RuntimeGson
public record PunishmentUpdateMessage(Action action, Punishment punishment, String message) {

    public Component component() {
        return MiniMessage.miniMessage().deserialize(this.message);
    }

    // Serialized as ordinal
    public enum Action {
        CREATE,
        REVOKE
    }
}
