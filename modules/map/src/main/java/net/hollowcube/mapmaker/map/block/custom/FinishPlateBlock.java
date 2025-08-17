package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.object.ObjectBlockHandler;
import net.hollowcube.mapmaker.map.object.ObjectTypes;
import net.hollowcube.mapmaker.object.ObjectType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class FinishPlateBlock implements ObjectBlockHandler, PressurePlateBlockMixin {

    public static final BlockItemHandler ITEM = new BlockItemHandler(FinishPlateBlock::new,
            Block.LIGHT_WEIGHTED_PRESSURE_PLATE, "finish_plate");

    private final Set<Player> playersOnPlate = new HashSet<>();

    @Override
    public @NotNull ObjectType objectType() {
        return ObjectTypes.FINISH_PLATE;
    }

    @Override
    public @NotNull Set<Player> getPlayersOnPlate() {
        return playersOnPlate;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        var finishId = createObjectId(tick.getBlockPosition());
        world.callEvent(new MapPlayerCompleteMapEvent(player, world, finishId));
    }

}
