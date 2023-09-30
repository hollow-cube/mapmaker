package net.hollowcube.map.feature.checkpoint;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.feature.checkpoint.gui.CheckpointSettingsView;
import net.hollowcube.map.item.BlockItemHandler;
import net.hollowcube.map.object.ObjectBlockHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.object.ObjectType;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CheckpointPlateBlock implements ObjectBlockHandler, PressurePlateBlockMixin {
    public static final ObjectType OBJECT_TYPE = ObjectType.builder("mapmaker:checkpoint_plate")
            .requiredVariant(MapVariant.PARKOUR)
            .build();

    public static final CheckpointPlateBlock INSTANCE = new CheckpointPlateBlock();
    public static final Block VANILLA_BLOCK = Block.HEAVY_WEIGHTED_PRESSURE_PLATE;
    public static final BlockItemHandler ITEM = new BlockItemHandler(INSTANCE, VANILLA_BLOCK, CheckpointPlateBlock::updateItemStack);

    @Override
    public @NotNull ObjectType objectType() {
        return OBJECT_TYPE;
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var world = MapWorld.forPlayer(interaction.getPlayer());
        if ((world.flags() & MapWorld.FLAG_EDITING) == 0) return false;

        var player = interaction.getPlayer();
        if (interaction.getHand() != Player.Hand.MAIN || player.isSneaking()) return false;

        // Open checkpoint settings GUI
        world.server().newOpenGUI(player, c -> new CheckpointSettingsView(c,
                interaction.getInstance(), interaction.getBlockPosition(), interaction.getBlock()));

        return true;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var mapWorld = MapWorld.forPlayer(player);
        var event = new MapWorldCheckpointReachedEvent(mapWorld, player, createObjectId(tick.getBlockPosition()));
        EventDispatcher.call(event);
    }

    public static void updateItemStack(ItemStack.@NotNull Builder builder, @NotNull TagHandler tag) {
        var args = new ArrayList<Component>();
        var isEmpty = true;

        int resetHeight = tag.getTag(CheckpointSetting.RESET_HEIGHT);
        args.add(CheckpointSetting.RESET_HEIGHT_TEXT_FUNCTION.apply(resetHeight));
        if (resetHeight != -1) isEmpty = false;

        // If the NBT has settings, set the lore to the "with data" variant, otherwise leave the default.
        if (!isEmpty) builder.lore(LanguageProviderV2.translateMulti("item.mapmaker.checkpoint_plate.with_data.lore", args));
        builder.meta(m -> m.setTag(BlockItemHandler.BLOCK_DATA, tag.asCompound()));
    }

}
