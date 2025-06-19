package net.hollowcube.compat.api;

import net.hollowcube.common.util.ProtocolVersions;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ModChannelRegisterEvent implements PlayerEvent {
    private final Player player;
    private final List<String> channels;
    private final List<Component> disabledMods = new ArrayList<>();

    public ModChannelRegisterEvent(@NotNull Player player, @NotNull List<String> channels) {
        this.player = player;
        this.channels = channels;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull List<Component> getDisabledMods() {
        return disabledMods;
    }

    public int getPlayerProtocolVersion() {
        return ProtocolVersions.getProtocolVersion(player);
    }

    public void excludeNamespace(@NotNull Component modName, @NotNull String... namespaces) {
        disabledMods.add(modName);
        for (String namespace : namespaces) {
            channels.removeIf(channel -> channel.startsWith(namespace + ":"));
        }
    }
}
