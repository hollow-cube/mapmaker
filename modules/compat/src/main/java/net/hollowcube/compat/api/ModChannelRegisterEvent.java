package net.hollowcube.compat.api;

import net.hollowcube.common.util.ProtocolVersions;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

public class ModChannelRegisterEvent implements PlayerEvent {
    private final Player player;
    private final List<String> channels;
    private final List<Component> disabledMods = new ArrayList<>();

    public ModChannelRegisterEvent(Player player, List<String> channels) {
        this.player = player;
        this.channels = channels;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public List<Component> getDisabledMods() {
        return disabledMods;
    }

    public List<String> getChannels() {
        return channels;
    }

    public int getPlayerProtocolVersion() {
        return ProtocolVersions.getProtocolVersion(player);
    }

    public boolean excludeNamespace(String... namespaces) {
        boolean removed = false;
        for (String namespace : namespaces) {
            removed |= channels.removeIf(channel -> channel.startsWith(namespace + ":"));
        }
        return removed;
    }

    public void excludeNamespace(Component modName, String... namespaces) {
        if (excludeNamespace(namespaces)) {
            disabledMods.add(modName);
        }
    }
}
