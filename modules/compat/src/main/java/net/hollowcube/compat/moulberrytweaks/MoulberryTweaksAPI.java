package net.hollowcube.compat.moulberrytweaks;

import net.hollowcube.compat.impl.PacketQueue;
import net.minestom.server.entity.Player;

public final class MoulberryTweaksAPI {
    public static final String DEBUG_RENDER_CHANNEL = "debugrender";

    public static boolean isPresent(Player player) {
        return PacketQueue.get(player).channels().stream()
            .anyMatch(channel -> channel.startsWith(DEBUG_RENDER_CHANNEL));
    }

    private MoulberryTweaksAPI() {
    }

}
