package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.entity.impl.other.ArmorStandEntity;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ArmorStandInteractionRule implements BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        if (interaction.blockFace() == BlockFace.BOTTOM) return false;

        var position = interaction.blockPosition().relative(interaction.blockFace()).add(0.5, 0, 0.5);

        var entity = new ArmorStandEntity(UUID.randomUUID());

        var rot = (float) Math.floor(interaction.player().getPosition().yaw() / 45) * 45f + 180;
        entity.setInstance(interaction.instance(), new Pos(
                position,
                rot,
                0
        ));

        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }
}
