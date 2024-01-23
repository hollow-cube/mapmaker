package net.hollowcube.map.block.interaction;

import net.hollowcube.map.block.BlockTags;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public class ButtonInteractionRule extends AbstractToggleInteractionRule {

    @Override
    protected void playSound(@NotNull Interaction interaction, @NotNull Block block, boolean newState) {
        SoundEvent soundEvent = null;
        if (BlockTags.WOODEN_BUTTONS.contains(block.namespace())) {
            soundEvent = newState ? SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_OFF : SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON;
        } else if (BlockTags.STONE_BUTTONS.contains(block.namespace())) {
            soundEvent = newState ? SoundEvent.BLOCK_STONE_BUTTON_CLICK_OFF : SoundEvent.BLOCK_STONE_BUTTON_CLICK_ON;
        } else if (BlockTags.NETHER_WOOD_BUTTONS.contains(block.namespace())) {
            soundEvent = newState ? SoundEvent.BLOCK_NETHER_WOOD_BUTTON_CLICK_OFF : SoundEvent.BLOCK_NETHER_WOOD_BUTTON_CLICK_ON;
        } else if (Block.CHERRY_BUTTON.id() == block.id()) {
            soundEvent = newState ? SoundEvent.BLOCK_CHERRY_WOOD_BUTTON_CLICK_OFF : SoundEvent.BLOCK_CHERRY_WOOD_BUTTON_CLICK_ON;
        } else if (Block.BAMBOO_BUTTON.id() == block.id()) {
            soundEvent = newState ? SoundEvent.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_OFF : SoundEvent.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON;
        }

        Check.notNull(soundEvent, "soundEvent (" + block.namespace() + ")");
        if (newState) {
            // The client predicts pressing buttons for itself only, so need to send the sound only to viewers
            var viewers = interaction.player().getViewersAsAudience();
            var blockPosition = interaction.blockPosition();
            viewers.playSound(Sound.sound(soundEvent, Sound.Source.BLOCK, 1.0f, 1.0f),
                    blockPosition.x(), blockPosition.y(), blockPosition.z());
        } else {
            // This is not predicted at all, send to everyone
            interaction.playBlockSound(soundEvent, 1.0f, 1.0f);
        }
    }

}
