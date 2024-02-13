package net.hollowcube.terraform.compat.worldedit.script;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.suggestion.SuggestionEntry;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.hollowcube.terraform.util.script.Assertions.assertSuggestions;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WEPatternSuggestTest {

    @Nested
    class BlockStateTest {

        @Test
        void testSuggestEmpty() {
            assertSuggestions(
                    new PatternTree.BlockState(
                            new PatternTree.NamespaceId(0, 0, -1, "", null),
                            new PatternTree.PropertyList(-1, -1, -1, -1, -1, List.of())),
                    "acacia_button", "acacia_door", "acacia_fence", "acacia_fence_gate", "acacia_hanging_sign",
                    "acacia_leaves", "acacia_log", "acacia_planks", "acacia_pressure_plate", "acacia_sapling",
                    "acacia_sign", "acacia_slab", "acacia_stairs", "acacia_trapdoor", "acacia_wall_hanging_sign",
                    "acacia_wall_sign", "acacia_wood", "activator_rail", "air", "allium"
            );
        }

        @Test
        void testSuggestBlockStatePartialPath() {
            assertSuggestions(
                    new PatternTree.BlockState(
                            new PatternTree.NamespaceId(0, 4, -1, "ston", null),
                            new PatternTree.PropertyList(-1, -1, -1, -1, -1, List.of())),
                    "stone", "stone_brick_slab", "stone_brick_stairs", "stone_brick_wall", "stone_bricks",
                    "stone_button", "stone_pressure_plate", "stone_slab", "stone_stairs", "stonecutter"
            );
        }

        @Test
        void testSuggestBlockStatePartialNamespace() {
            assertSuggestions(
                    new PatternTree.BlockState(
                            new PatternTree.NamespaceId(0, 5, -1, "minec", null),
                            new PatternTree.PropertyList(-1, -1, -1, -1, -1, List.of())),
                    "minecraft:"
            );
        }

        @Test
        void testSuggestBlockStateFullNamespace() {
            assertSuggestions(
                    new PatternTree.BlockState(
                            new PatternTree.NamespaceId(0, 10, 9, "minecraft:", null),
                            new PatternTree.PropertyList(-1, -1, -1, -1, -1, List.of())),
                    "minecraft:acacia_button", "minecraft:acacia_door", "minecraft:acacia_fence", "minecraft:acacia_fence_gate", "minecraft:acacia_hanging_sign",
                    "minecraft:acacia_leaves", "minecraft:acacia_log", "minecraft:acacia_planks", "minecraft:acacia_pressure_plate", "minecraft:acacia_sapling",
                    "minecraft:acacia_sign", "minecraft:acacia_slab", "minecraft:acacia_stairs", "minecraft:acacia_trapdoor", "minecraft:acacia_wall_hanging_sign",
                    "minecraft:acacia_wall_sign", "minecraft:acacia_wood", "minecraft:activator_rail", "minecraft:air", "minecraft:allium"
            );
        }

        @Test
        void testBlockStateFullMatchWithPossibleProps() {
            assertSuggestions(
                    new PatternTree.BlockState(
                            new PatternTree.NamespaceId(0, 12, -1, "stone_stairs", null),
                            new PatternTree.PropertyList(-1, -1, -1, -1, -1, List.of())),
                    "["
            );
        }

        @Test
        void testBlockStateFullMatchNoProps() {
            assertSuggestions(
                    new PatternTree.BlockState(
                            new PatternTree.NamespaceId(0, 5, -1, "stone", null),
                            new PatternTree.PropertyList(-1, -1, -1, -1, -1, List.of()))
            );
        }
    }

    //todo weighted + list

    @Nested
    class TestPropertyList {
        @Test
        void testBlockWithNoProps() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 1, 0, -1, -1, List.of()),
                    Block.STONE, "]"
            );
        }

        @Test
        void testSinglePropNoInput() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 1, 0, -1, -1, List.of()),
                    Block.WATER_CAULDRON, "level"
            );
        }

        @Test
        void testMultiPropStartNewPropEmpty() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of()),
                    Block.STONE_STAIRS, "facing", "half", "shape", "waterlogged"
            );
        }

        @Test
        void testMultiPropStartNewPropNoDuplicate() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 13, 0, -1, 12, List.of(
                            new PatternTree.PropertyList.Property(1, 11, -1, "facing", "east")
                    )), Block.STONE_STAIRS, "half", "shape", "waterlogged"
            );
        }

        @Test
        void testPartialPropInvalidName() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, -1, "d", null)
                    )), Block.STONE_STAIRS
            );
        }

        @Test
        void testSinglePropCompleteMatch() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, 2, "half", "bottom")
                    )), Block.STONE_STAIRS, "]"
            );
        }

        @Test
        void testSinglePropKeyMatchEmpty() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, 2, "shape", null)
                    )), Block.STONE_STAIRS, "inner_left", "inner_right", "outer_left", "outer_right", "straight"
            );
        }

        @Test
        void testSinglePropKeyMatchPartial() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, 2, "shape", "in")
                    )), Block.STONE_STAIRS, "inner_left", "inner_right"
            );
        }

        @Test
        void testSinglePropKeyMatchNoEquals() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, -1, "half", null)
                    )), Block.STONE_STAIRS, "="
            );
        }

        @Test
        void testSinglePropKeyPartialMatchNoEquals() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, -1, "ha", null)
                    )), Block.STONE_STAIRS, "half"
            );
        }

        @Test
        void testRegression() {
            assertPropertyListSuggestions(
                    new PatternTree.PropertyList(0, 4, 0, -1, -1, List.of(
                            new PatternTree.PropertyList.Property(1, 11, 2, "age", "12")
                    )), null, "]"
            );
        }

        public static void assertPropertyListSuggestions(@NotNull PatternTree.PropertyList tree, @Nullable Block block, @NotNull String... expected) {
            var suggestion = new Suggestion(0, 0);
            tree.suggest(suggestion, block);
            var actual = suggestion.getEntries().stream().map(SuggestionEntry::replacement).toList();
            assertEquals(List.of(expected), actual);
        }

    }

}
