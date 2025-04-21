package net.hollowcube.mapmaker.map.block.custom;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.map.MapIntegrationTest;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MapIntegrationTest
class CheckpointPlateBlockIntegrationTest {

    @Test
    void checkpointTeleport(Env env, MapWorld world, Player player) {
        var expected = new Pos(5, 5, 5);
        placeCheckpoint(world, c -> c.setTeleport(expected), null);

        env.tick();
        assertEquals(expected, player.getPosition());
    }

    @Test
    void checkpointResetHeight(Env env, MapWorld world, Player player) {
        placeCheckpoint(world, c -> c.setResetHeight(12), null);

        env.listen(world.eventNode(), MapPlayerCheckpointChangeEvent.class)
                .followup(); // Ensure checkpoint change is called
        env.tick(); // Step on plate

        env.listen(world.eventNode(), MapPlayerInitEvent.class)
                .followup(); // Ensure the player is reset
        player.teleport(new Pos(0, 0, 0)); // Below reset height
        env.tick();

        // Should end at original position
        assertEquals(new Pos(0, 40, 0), player.getPosition());
    }

    private static void placeCheckpoint(@NotNull MapWorld world, @NotNull Consumer<CheckpointEffectData> editor, @Nullable Point position) {
        var checkpoint = CheckpointEffectData.CODEC.decode(Transcoder.JSON, new JsonObject()).orElseThrow();
        editor.accept(checkpoint);
        var block = CheckpointPlateBlock.ITEM.block().withTag(CheckpointPlateBlock.DATA_TAG, checkpoint);
        world.instance().setBlock(Objects.requireNonNullElse(position, new Pos(0, 40, 0)), block);
    }
}
