package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointPostChangeEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointPreChangeEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.hollowcube.mapmaker.map.gui.effect.EditCheckpointView;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.object.ObjectBlockHandler;
import net.hollowcube.mapmaker.map.object.ObjectTypes;
import net.hollowcube.mapmaker.map.util.InteractTarget;
import net.hollowcube.mapmaker.object.ObjectType;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CheckpointPlateBlock implements ObjectBlockHandler, InteractTarget, PressurePlateBlockMixin, DebugCommand.BlockDebug {
    private static final Tag<CheckpointEffectData> DATA_TAG = DFU.View(CheckpointEffectData.CODEC);
    public static final Tag<CheckpointEffectData> ENTITY_DATA_TAG = DFU.Tag(CheckpointEffectData.CODEC, "checkpoint").path("data");

    public static final BlockItemHandler ITEM = new BlockItemHandler(CheckpointPlateBlock::new,
            Block.HEAVY_WEIGHTED_PRESSURE_PLATE, "checkpoint_plate", CheckpointPlateBlock::updateItemStack);

    private final Set<Player> playersOnPlate = new HashSet<>();

    @Override
    public @NotNull ObjectType objectType() {
        return ObjectTypes.CHECKPOINT_PLATE;
    }

    @Override
    public @NotNull Set<Player> getPlayersOnPlate() {
        return playersOnPlate;
    }

    public void editData(@NotNull Instance instance, @NotNull Point blockPosition, @NotNull Block block, @NotNull Consumer<CheckpointEffectData> func) {
        var data = block.getTag(DATA_TAG);
        func.accept(data);

        var newTag = TagHandler.newHandler();
        newTag.setTag(DATA_TAG, data);
        instance.setBlock(blockPosition, block.withNbt(newTag.asCompound()));
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var player = interaction.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return true;
        if (world.itemRegistry().isOnCooldown(player)) return true;

        if (interaction.getHand() != PlayerHand.MAIN || player.isSneaking()) return true;

        // Open checkpoint settings GUI
        var data = interaction.getBlock().getTag(DATA_TAG);
        var maxResetHeight = interaction.getBlockPosition().blockY();
        world.server().showView(player, c -> new EditCheckpointView(c.with(Map.of("updateTarget", interaction.getBlockPosition())), data, maxResetHeight, () -> {
            var instance = interaction.getInstance();
            var blockPosition = interaction.getBlockPosition();

            var newNbt = DFU.encodeNbt(CheckpointEffectData.CODEC, data);
            instance.setBlock(blockPosition, interaction.getBlock().withNbt(newNbt));
        }));

        return false;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        var data = tick.getBlock().getTag(DATA_TAG);
        var checkpointId = createObjectId(tick.getBlockPosition());
        world.callEvent(new MapPlayerCheckpointPreChangeEvent(player, world, checkpointId, data));
    }

    @Override
    public void onPlateReleased(@NotNull Tick tick, @NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        var data = tick.getBlock().getTag(DATA_TAG);
        var checkpointId = createObjectId(tick.getBlockPosition());
        world.callEvent(new MapPlayerCheckpointPostChangeEvent(player, world, checkpointId, data));
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
