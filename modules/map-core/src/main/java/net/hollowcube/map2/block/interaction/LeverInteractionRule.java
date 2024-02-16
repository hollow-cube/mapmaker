package net.hollowcube.map2.block.interaction;

import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

public class LeverInteractionRule extends net.hollowcube.map2.block.interaction.AbstractToggleInteractionRule {

    @Override
    protected void playSound(@NotNull Interaction interaction, @NotNull Block block, boolean newState) {
        interaction.playBlockSound(SoundEvent.BLOCK_LEVER_CLICK, 0.3f, newState ? 0.6f : 0.5f);
    }

}
