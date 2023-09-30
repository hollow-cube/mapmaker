package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.item.BlockItemHandler;
import net.hollowcube.map.object.ObjectBlockHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.object.ObjectType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class FinishPlateBlock implements ObjectBlockHandler, PressurePlateBlockMixin {
    public static final ObjectType OBJECT_TYPE = ObjectType.builder("mapmaker:finish_plate")
            .requiredVariant(MapVariant.PARKOUR)
            .build();

    public static final FinishPlateBlock INSTANCE = new FinishPlateBlock();
    public static final BlockItemHandler ITEM = new BlockItemHandler(INSTANCE, Block.LIGHT_WEIGHTED_PRESSURE_PLATE);

    @Override
    public @NotNull ObjectType objectType() {
        return OBJECT_TYPE;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        EventDispatcher.call(new MapWorldCompleteEvent(MapWorld.forPlayer(player), player));
    }

}
