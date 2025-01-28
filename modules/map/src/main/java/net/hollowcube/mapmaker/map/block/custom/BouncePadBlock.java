package net.hollowcube.mapmaker.map.block.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.DFU;
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
import java.util.Optional;
import java.util.Set;

public class BouncePadBlock implements BlockHandler, PressurePlateBlockMixin, DebugCommand.BlockDebug {
    private static final NamespaceID ID = NamespaceID.from("mapmaker:bounce_pad");
    private static final Tag<Data> DATA_TAG = DFU.View(Data.CODEC);
    public static final Tag<Data> ENTITY_DATA_TAG = DFU.Tag(Data.CODEC, "bounce_pad").path("data");

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
    public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
        applyVelocity(tick.getBlock().getTag(DATA_TAG), player);
    }

    @Override
    public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
        var data = block.getTag(DATA_TAG);
        player.sendMessage("Power: " + data.power());
        player.sendMessage("Direction: " + data.pitch().map(String::valueOf).orElse("none") +
                ", " + data.yaw().map(String::valueOf).orElse("none"));
    }

    public static void applyVelocity(@NotNull Data data, @NotNull Player player) {
        if (data.legacyCyloneMode) {
            var velocity = player.getPosition().sub(player.getPreviousPosition());
            player.setVelocity(Vec.fromPoint(velocity.withY(1.45f * 20)));
            return;
        }

        var boostVelocity = player.getPosition().direction().withY(1).mul(data.power());
        player.setVelocity(boostVelocity);
    }

    public record Data(double power, @NotNull Optional<Float> pitch, @NotNull Optional<Float> yaw,
                       boolean legacyCyloneMode) {
        private static final double DEFAULT_POWER = 25;

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.optionalFieldOf("power", DEFAULT_POWER).forGetter(Data::power),
                Codec.FLOAT.optionalFieldOf("pitch").forGetter(Data::pitch),
                Codec.FLOAT.optionalFieldOf("yaw").forGetter(Data::yaw),
                Codec.BOOL.optionalFieldOf("legacy_cylone_mode", false).forGetter(Data::legacyCyloneMode)
        ).apply(i, Data::new));
    }
}
