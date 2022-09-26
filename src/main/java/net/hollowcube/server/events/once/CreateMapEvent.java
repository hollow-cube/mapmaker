package net.hollowcube.server.events.once;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import omega.mapmaker.MapMaker;

public class CreateMapEvent {
    public static void onCreateMap(Player player, String mapName) {
        InstanceContainer map =
                MapMaker.getInstance().getWorldInstanceManager().getInstanceManager().createInstanceContainer();
        map.setBlock(0, 58, 0, Block.WHITE_WOOL);
        player.setInstance(map, new Pos(0, 60, 0));
        player.sendMessage(
                Component.text("Successfully created ", NamedTextColor.WHITE)
                        .append(Component.text(mapName, NamedTextColor.AQUA)));
    }
}
