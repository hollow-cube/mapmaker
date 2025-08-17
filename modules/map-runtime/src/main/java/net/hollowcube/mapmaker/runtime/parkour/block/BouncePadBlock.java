package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.marker.bouncepad.BouncePadData;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

public class BouncePadBlock implements BlockHandler, PressurePlateBlock, DebugCommand.BlockDebug {
    private static final Tag<BouncePadData> DATA_TAG = DFU.View(BouncePadData.CODEC);

    public static final Key KEY = Key.key("mapmaker:bounce_pad");

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public void onPlace(Placement placement) {
        var data = placement.getBlock().getTag(DATA_TAG);
        if (data == null || !(placement instanceof PlayerPlacement p)) return;
        data.onUpdate(p.getPlayer());
    }

    @Override
    public void onEnter(Collision collision) {
        if (!(collision.world() instanceof ParkourMapWorld world)) return;
        if (!(world.getPlayerState(collision.player()) instanceof ParkourState.AnyPlaying)) return;

        applyVelocity(collision.block().getTag(DATA_TAG), collision.player());
    }

    @Override
    public void sendDebugInfo(Player player, Block block) {
        var data = block.getTag(DATA_TAG);
        if (data == null) return;
        data.sendDebugInfo(player, block);
    }

    public static void applyVelocity(BouncePadData data, Player player) {
        var newVelocity = data.getVelocity(player);
        if (newVelocity == null) return;
        player.setVelocity(new Vec(
                Math.min(Math.max(newVelocity.x(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY),
                Math.min(Math.max(newVelocity.y(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY),
                Math.min(Math.max(newVelocity.z(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY)
        ));
    }

}
