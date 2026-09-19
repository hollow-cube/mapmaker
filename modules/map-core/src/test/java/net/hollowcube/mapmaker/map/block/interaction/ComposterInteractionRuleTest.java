package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Compostable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposterInteractionRuleTest {

    static {
        MinecraftServer.init();
    }

    @Test
    void vanillaCompostablesAreCompostable() {
        assertTrue(ComposterInteractionRule.isCompostable(ItemStack.of(Material.OAK_LEAVES)));
        assertTrue(ComposterInteractionRule.isCompostable(ItemStack.of(Material.PUMPKIN_PIE)));
        assertTrue(ComposterInteractionRule.isCompostable(ItemStack.of(Material.RED_SHRUB)));
        assertFalse(ComposterInteractionRule.isCompostable(ItemStack.of(Material.STONE)));
    }

    @Test
    void literalLayerCountIsRespected() {
        assertTrue(ComposterInteractionRule.isCompostable(ItemStack.of(Material.STONE).with(DataComponents.COMPOSTABLE, new Compostable(2))));
        assertFalse(ComposterInteractionRule.isCompostable(ItemStack.of(Material.OAK_LEAVES).with(DataComponents.COMPOSTABLE, new Compostable(0))));
    }
}
