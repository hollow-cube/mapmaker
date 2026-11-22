package net.hollowcube.mapmaker.util;

import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutocompletorsTest {

    @Test
    void soundsMatchWordsInsideDottedIds() {
        var anvil = Autocompletors.searchSounds("anvil", 27);
        assertEquals(8, anvil.size());
        assertTrue(anvil.stream().allMatch(s -> s.key().value().startsWith("block.anvil.")));

        assertEquals(List.of(SoundEvent.BLOCK_ANVIL_LAND), Autocompletors.searchSounds("anvil land", 27));
        assertEquals(List.of(SoundEvent.BLOCK_NOTE_BLOCK_HARP), Autocompletors.searchSounds("note harp", 27));
        assertEquals(List.of(SoundEvent.MUSIC_DISC_PIGSTEP), Autocompletors.searchSounds("pigstep", 27));
        assertEquals(SoundEvent.ENTITY_PLAYER_LEVELUP, Autocompletors.searchSounds("level up", 27).getFirst());
    }

    @Test
    void soundsRankWholeSegmentsFirst() {
        assertTrue(Autocompletors.searchSounds("cat", 27).getFirst().key().value().startsWith("entity.cat."));
        assertTrue(Autocompletors.searchSounds("zombie", 27).getFirst().key().value().startsWith("entity.zombie."));
    }

    @Test
    void soundsTolerateTyposAndStems() {
        assertTrue(Autocompletors.searchSounds("villagr", 27).getFirst().key().value().startsWith("entity.villager."));
        assertTrue(Autocompletors.searchSounds("explosion", 27).contains(SoundEvent.ENTITY_GENERIC_EXPLODE));
    }

    @Test
    void soundsEmptyQueryAndLimit() {
        assertEquals(List.of(), Autocompletors.searchSounds("  ", 27));
        assertEquals(3, Autocompletors.searchSounds("a", 3).size());
    }

    @Test
    void materialsMatchWordsInsideUnderscoredIds() {
        assertEquals(Material.OAK_PLANKS, Autocompletors.searchMaterials("oak planks", 27, _ -> true).getFirst());
        assertTrue(Autocompletors.searchMaterials("planks", 27, _ -> true).contains(Material.BIRCH_PLANKS));
        assertEquals(Material.STONE, Autocompletors.searchMaterials("stone", 27, _ -> true).getFirst());
    }

    @Test
    void materialsPredicateAppliesBeforeLimit() {
        var results = Autocompletors.searchMaterials("planks", 3, m -> m != Material.OAK_PLANKS);
        assertEquals(3, results.size());
        assertFalse(results.contains(Material.OAK_PLANKS));
    }

    @Test
    void blocksSearch() {
        assertEquals(Block.OAK_PLANKS, Autocompletors.searchBlocks("oak planks", 27).getFirst());
    }
}
