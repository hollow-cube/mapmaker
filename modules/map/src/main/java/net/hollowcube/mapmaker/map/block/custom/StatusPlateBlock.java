package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.handler.PressurePlateBlockMixin;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerStatusChangeEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.map.gui.effect.EditStatusView;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.object.ObjectBlockHandler;
import net.hollowcube.mapmaker.map.util.InteractTarget;
import net.hollowcube.mapmaker.object.ObjectType;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class StatusPlateBlock implements ObjectBlockHandler, InteractTarget, PressurePlateBlockMixin, DebugCommand.BlockDebug {
    private static final Tag<StatusEffectData> DATA_TAG = DFU.View(StatusEffectData.CODEC);
    public static final Tag<StatusEffectData> ENTITY_DATA_TAG = DFU.Tag(StatusEffectData.CODEC, "status").path("data");

    public static final TagCooldown APPLY_COOLDOWN = new TagCooldown("mapmaker:status_plate_cooldown", 250);

    public static final ObjectType OBJECT_TYPE = ObjectType.builder("mapmaker:status_plate")
            .requiredVariant(MapVariant.PARKOUR)
            .build();

    public static final ItemHandler ITEM = new BlockItemHandler(StatusPlateBlock::new, Block.STONE_PRESSURE_PLATE);

    private final Set<Player> playersOnPlate = new HashSet<>();

    @Override
    public @NotNull ObjectType objectType() {
        return OBJECT_TYPE;
    }

    @Override
    public @NotNull Set<Player> getPlayersOnPlate() {
        return playersOnPlate;
    }

    public void editData(@NotNull Instance instance, @NotNull Point blockPosition, @NotNull Block block, @NotNull Consumer<StatusEffectData> func) {
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
        world.server().showView(player, c -> new EditStatusView(c.with(Map.of("updateTarget", interaction.getBlockPosition())), data, maxResetHeight, () -> {
            var instance = interaction.getInstance();
            var blockPosition = interaction.getBlockPosition();

            var newNbt = DFU.encodeNbt(StatusEffectData.CODEC, data);
            instance.setBlock(blockPosition, interaction.getBlock().withNbt(newNbt));
        }));

        return false;
    }

    @Override
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;

        if (APPLY_COOLDOWN.test(player)) {
            var data = tick.getBlock().getTag(DATA_TAG);
            var statusId = createObjectId(tick.getBlockPosition());
            world.callEvent(new MapPlayerStatusChangeEvent(player, world, statusId, data));
        }
    }

    @Override
    public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
        block.getTag(DATA_TAG).sendDebugInfo(player);
    }
}
