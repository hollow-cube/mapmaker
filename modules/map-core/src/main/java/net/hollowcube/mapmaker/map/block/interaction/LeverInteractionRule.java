package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;

public class LeverInteractionRule extends AbstractToggleInteractionRule {

    @Override
    protected void playSound(Interaction interaction, Block block, boolean newState) {
        interaction.playBlockSound(SoundEvent.BLOCK_LEVER_CLICK, 0.3f, newState ? 0.6f : 0.5f);
    }

}
