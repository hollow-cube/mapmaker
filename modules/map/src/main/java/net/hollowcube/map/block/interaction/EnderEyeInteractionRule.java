package net.hollowcube.map.block.interaction;

import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class EnderEyeInteractionRule implements BlockInteractionRule {
    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);

        if (block.id() != Block.END_PORTAL_FRAME.id()) return false;
        if ("true".equals(block.getProperty("eye"))) return false;

        interaction.playBlockSound(SoundEvent.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
        interaction.setBlock(blockPosition, block.withProperty("eye", "true"));
        return false;
    }
}
