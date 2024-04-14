package net.hollowcube.mapmaker.map.block.custom;

import net.hollowcube.mapmaker.entity.PlayerCooldown;
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
import net.hollowcube.mapmaker.object.ObjectType;
import net.hollowcube.mapmaker.util.dfu.DFU;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.time.Cooldown;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;

public class StatusPlateBlock implements ObjectBlockHandler, PressurePlateBlockMixin, DebugCommand.BlockDebug, PlayerCooldown {
    private static final Tag<StatusEffectData> DATA_TAG = DFU.View(StatusEffectData.CODEC);
    private static final Tag<Cooldown> APPLY_COOLDOWN_TAG = Tag.Transient("mapmaker:status_plate_cooldown");
    private static final Duration COOLDOWN_TIME = Duration.of(250L, ChronoUnit.MILLIS);

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

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var player = interaction.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return true;

        if (interaction.getHand() != Player.Hand.MAIN || player.isSneaking()) return true;

        // Open checkpoint settings GUI
        var data = interaction.getBlock().getTag(DATA_TAG);
        var maxResetHeight = interaction.getBlockPosition().blockY();
        world.server().showView(player, c -> new EditStatusView(c, data, maxResetHeight, () -> {
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
        tryUseCooldown(player, () -> {
            var data = tick.getBlock().getTag(DATA_TAG);
            var statusId = createObjectId(tick.getBlockPosition());
            world.callEvent(new MapPlayerStatusChangeEvent(player, world, statusId, data));
        });
    }

    @Override
    public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
        block.getTag(DATA_TAG).sendDebugInfo(player);
    }

    @Override
    public @NotNull Tag<Cooldown> cooldownTag() {
        return APPLY_COOLDOWN_TAG;
    }

    @Override
    public @NotNull Duration cooldownDuration() {
        return COOLDOWN_TIME;
    }
}
