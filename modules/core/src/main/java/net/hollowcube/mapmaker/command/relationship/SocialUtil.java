package net.hollowcube.mapmaker.command.relationship;

import net.hollowcube.ipc.player.SocialService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.UUID;

public final class SocialUtil {
    public static boolean failIfBlocked(
        SocialService social,
        Player player,
        String targetId,
        String targetUsername,
        boolean bidirectional
    ) {
        var playerId = player.getUuid();
        var blocks = social.blocksBetween(UUID.fromString(targetId), playerId, bidirectional);
        if (blocks.isEmpty()) return false;
        var key = blocks.getFirst().blockerId().equals(playerId)
            ? "generic.command.blocked_by_self" : "generic.command.blocked_by_target";
        player.sendMessage(Component.translatable(key, Component.text(targetUsername)));
        return true;
    }

    private SocialUtil() {}
}
