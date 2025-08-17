package net.hollowcube.mapmaker.map.block.custom;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.map.AbstractMapIntegrationTest;
import net.hollowcube.mapmaker.map.action.impl.ResetHeightAction;
import net.hollowcube.mapmaker.map.action.impl.TeleportAction;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectDataV2;
import net.hollowcube.mapmaker.map.util.RelativePos;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckpointPlateBlockIntegrationTest extends AbstractMapIntegrationTest {

    @Test
    void checkpointTeleport() {
        var expected = new Pos(5, 5, 5);
        placeCheckpoint(c -> c.actions().addAction(TeleportAction.KEY)
                .<TeleportAction>update(_ -> new TeleportAction(new RelativePos(expected, 0))), null);

        env.tick();
        assertEquals(expected, player.getPosition());
    }

    @Test
    void checkpointResetHeight() {
        placeCheckpoint(c -> c.actions()
                        .addAction(ResetHeightAction.KEY)
                        .<ResetHeightAction>update(it -> it.withValue(12)),
                null);

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

    private void placeCheckpoint(@NotNull Consumer<CheckpointEffectDataV2> editor, @Nullable Point position) {
        var checkpoint = CheckpointEffectDataV2.CODEC.decode(Transcoder.JSON, new JsonObject()).orElseThrow();
        editor.accept(checkpoint);
        var block = CheckpointPlateBlock.ITEM.block().withTag(CheckpointPlateBlock.DATA_TAG, checkpoint);
        world.instance().setBlock(Objects.requireNonNullElse(position, new Pos(0, 40, 0)), block);
    }

}
