package net.hollowcube.map.block.custom;

import net.hollowcube.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.map.feature.play.effect.StatusEffectData;
import net.hollowcube.map.gui.effect.EditStatusView;
import net.hollowcube.map.item.handler.BlockItemHandler;
import net.hollowcube.map.item.handler.ItemHandler;
import net.hollowcube.map.object.ObjectBlockHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.command.util.DebugCommand;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.object.ObjectType;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class StatusPlateBlock implements ObjectBlockHandler, PressurePlateBlockMixin, DebugCommand.BlockDebug {
    private static final Tag<StatusEffectData> DATA_TAG = DFU.View(StatusEffectData.CODEC);

    public static final ObjectType OBJECT_TYPE = ObjectType.builder("mapmaker:status_plate")
            .requiredVariant(MapVariant.PARKOUR)
            .build();

    public static final StatusPlateBlock INSTANCE = new StatusPlateBlock();
    public static final ItemHandler ITEM = new BlockItemHandler(INSTANCE, Block.STONE_PRESSURE_PLATE);

    private final Set<Player> playersOnPlate = new HashSet<>();

    @Override
    public @NotNull ObjectType objectType() {
        return OBJECT_TYPE;
    }

    @Override
    public @NotNull Set<Player> getPlayersOnPlate() {
        return playersOnPlate;
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var world = MapWorld.forPlayerOptional(interaction.getPlayer());
        if (world == null || (world.flags() & MapWorld.FLAG_EDITING) == 0) return true;

        var player = interaction.getPlayer();
        if (interaction.getHand() != Player.Hand.MAIN || player.isSneaking()) return true;

        // Open checkpoint settings GUI
        var data = interaction.getBlock().getTag(DATA_TAG);
        var maxResetHeight = interaction.getBlockPosition().blockY() - 1;
        world.server().newOpenGUI(player, c -> new EditStatusView(c, data, maxResetHeight, () -> {
            var instance = interaction.getInstance();
            var blockPosition = interaction.getBlockPosition();
            instance.setBlock(blockPosition, interaction.getBlock().withTag(DATA_TAG, data));
        }));

        return false;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        var data = tick.getBlock().getTag(DATA_TAG);
        var statusId = createObjectId(tick.getBlockPosition());
        world.callEvent(new MapPlayerStatusChangeEvent(player, world, statusId, data));
    }

    @Override
    public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
        block.getTag(DATA_TAG).sendDebugInfo(player);
    }

}
