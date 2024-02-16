package net.hollowcube.map2.block.interaction;

import net.hollowcube.map2.entity.impl.EndCrystalEntity;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EndCrystalInteractionRule implements net.hollowcube.map2.block.interaction
        .BlockInteractionRule {

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var position = interaction.blockPosition().relative(interaction.blockFace()).add(0.5, 0, 0.5);

        var entity = new EndCrystalEntity(UUID.randomUUID());
        entity.getEntityMeta().setShowingBottom(false);
        entity.setInstance(interaction.instance(), position);

        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.BOTH;
    }
}
