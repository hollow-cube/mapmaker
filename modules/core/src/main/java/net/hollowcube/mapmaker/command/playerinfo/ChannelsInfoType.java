package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.compat.impl.PacketQueue;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

class ChannelsInfoType extends PlayerInfoType.ForPlayer {

    private final boolean namespaces;

    public ChannelsInfoType(boolean namespaces) {
        this.namespaces = namespaces;
    }

    @Override
    public void execute(@NotNull Player user, @NotNull Player target) {
        PacketQueue queue = PacketQueue.get(target);
        if (queue == null) {
            user.sendMessage("No channels found for %s".formatted(target.getUsername()));
        } else {
            var channels = queue.channels().stream()
                    .map(s -> namespaces ? s.split(":")[0] : s)
                    .distinct()
                    .sorted()
                    .toList();
            Component message = Component.text((namespaces ? "Namespaces for (%s): " : "Channels for (%s): ").formatted(target.getUsername()))
                    .appendNewline()
                    .append(Component.text(String.join(", ", channels)));
            user.sendMessage(message);
        }
    }
}
