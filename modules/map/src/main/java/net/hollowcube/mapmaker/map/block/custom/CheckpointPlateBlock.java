package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.hollowcube.mapmaker.map.gui.effect.EditCheckpointView;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.object.ObjectBlockHandler;
import net.hollowcube.mapmaker.map.object.ObjectTypes;
import net.hollowcube.mapmaker.object.ObjectType;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CheckpointPlateBlock implements ObjectBlockHandler, PressurePlateBlockMixin, DebugCommand.BlockDebug {
    private static final Tag<CheckpointEffectData> DATA_TAG = DFU.View(CheckpointEffectData.CODEC);

    public static final BlockItemHandler ITEM = new BlockItemHandler(CheckpointPlateBlock::new,
            Block.HEAVY_WEIGHTED_PRESSURE_PLATE, CheckpointPlateBlock::updateItemStack);

    private final Set<Player> playersOnPlate = new HashSet<>();

    @Override
    public @NotNull ObjectType objectType() {
        return ObjectTypes.CHECKPOINT_PLATE;
    }

    @Override
    public @NotNull Set<Player> getPlayersOnPlate() {
        return playersOnPlate;
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var player = interaction.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return true;

        if (interaction.getHand() != Player.Hand.MAIN || player.isSneaking()) return true;

        // Open checkpoint settings GUI
        var data = interaction.getBlock().getTag(DATA_TAG);
        var maxResetHeight = interaction.getBlockPosition().blockY() - 1;
        world.server().showView(player, c -> new EditCheckpointView(c, data, maxResetHeight, () -> {
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
        var checkpointId = createObjectId(tick.getBlockPosition());
        world.callEvent(new MapPlayerCheckpointChangeEvent(player, world, checkpointId, data));
    }

    @Override
    public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
        block.getTag(DATA_TAG).sendDebugInfo(player);
    }

    public static void updateItemStack(ItemStack.@NotNull Builder builder, @NotNull TagHandler tag) {
        var args = new ArrayList<Component>();
        var isEmpty = true;

        //todo reimplement

//        int resetHeight = tag.getTag(CheckpointSetting.RESET_HEIGHT);
//        args.add(CheckpointSetting.RESET_HEIGHT_TEXT_FUNCTION.apply(resetHeight));
//        if (resetHeight != -1) isEmpty = false;
//
//        // If the NBT has settings, set the lore to the "with data" variant, otherwise leave the default.
//        if (!isEmpty)
//            builder.lore(LanguageProviderV2.translateMulti("item.mapmaker.checkpoint_plate.with_data.lore", args)); //todo this translation key is remove, don't use it
//        builder.meta(m -> m.setTag(BlockItemHandler.BLOCK_DATA, tag.asCompound()));
    }

}
