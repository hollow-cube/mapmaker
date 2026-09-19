package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.block.BlockTags;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ButtonInteractionRuleTest {

    @Test
    void everyButtonHasClickSound() {
        for (var key : BlockTags.BUTTONS) {
            var block = Objects.requireNonNull(Block.fromKey(key));
            assertNotNull(ButtonInteractionRule.clickSound(block, true), key.asString());
            assertNotNull(ButtonInteractionRule.clickSound(block, false), key.asString());
        }
    }

    @Test
    void woodSetsWithCustomSoundsKeepThem() {
        assertEquals(SoundEvent.BLOCK_WOODEN_BUTTON_CLICK_ON, ButtonInteractionRule.clickSound(Block.POPLAR_BUTTON, false));
        assertEquals(SoundEvent.BLOCK_CHERRY_WOOD_BUTTON_CLICK_ON, ButtonInteractionRule.clickSound(Block.CHERRY_BUTTON, false));
        assertEquals(SoundEvent.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, ButtonInteractionRule.clickSound(Block.BAMBOO_BUTTON, false));
        assertEquals(SoundEvent.BLOCK_NETHER_WOOD_BUTTON_CLICK_ON, ButtonInteractionRule.clickSound(Block.WARPED_BUTTON, false));
        assertEquals(SoundEvent.BLOCK_STONE_BUTTON_CLICK_ON, ButtonInteractionRule.clickSound(Block.STONE_BUTTON, false));
    }
}
