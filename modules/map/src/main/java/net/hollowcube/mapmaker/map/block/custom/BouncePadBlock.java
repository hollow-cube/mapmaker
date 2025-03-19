package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.block.custom.bouncepad.BouncePadData;
import net.hollowcube.mapmaker.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class BouncePadBlock implements BlockHandler, PressurePlateBlockMixin, DebugCommand.BlockDebug {
    private static final NamespaceID ID = NamespaceID.from("mapmaker:bounce_pad");
    private static final Tag<BouncePadData> DATA_TAG = DFU.View(BouncePadData.CODEC);

    public static final ItemHandler ITEM = new BlockItemHandler(BouncePadBlock::new, Block.CHERRY_PRESSURE_PLATE);

    private final Set<Player> playersOnPlate = new HashSet<>();

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public @NotNull Set<Player> getPlayersOnPlate() {
        return playersOnPlate;
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        var data = placement.getBlock().getTag(DATA_TAG);
        if (data == null || !(placement instanceof PlayerPlacement p)) return;
        data.onUpdate(p.getPlayer());
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        applyVelocity(tick.getBlock().getTag(DATA_TAG), player);
    }

    @Override
    public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
        var data = block.getTag(DATA_TAG);
        if (data == null) return;
        data.sendDebugInfo(player, block);
    }

    public static void applyVelocity(@NotNull BouncePadData data, @NotNull Player player) {
        var newVelocity = data.getVelocity(player);
        if (newVelocity == null) return;
        player.setVelocity(new Vec(
                Math.min(Math.max(newVelocity.x(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY),
                Math.min(Math.max(newVelocity.y(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY),
                Math.min(Math.max(newVelocity.z(), -BouncePadData.MAX_VELOCITY), BouncePadData.MAX_VELOCITY)
        ));
    }
}
