package net.hollowcube.terraform.compat.worldedit.script;

import net.hollowcube.terraform.TerraformRegistry;
import net.hollowcube.terraform.pattern.*;
import net.hollowcube.terraform.util.script.ParseException;
import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WEPatternBuildTest {

    @Nested
    class TestBlockState {
        @Test
        public void testPlainStateValid() {
            var pattern = assertBuilds(BlockPattern.class, "stone");
            assertEquals(Block.STONE, pattern.block());
        }

        @Test
        public void testPlainStateEmptyPropsValid() {
            var pattern = assertBuilds(BlockPattern.class, "stone[]");
            assertEquals(Block.STONE, pattern.block());
        }

        @Test
        public void testPlainStateInvalid() {
            assertDoesNotBuild("no such block: minecraft:not_a_block", "not_a_block");
        }

        @Test
        public void testOpenBracketNoClose() {
            assertDoesNotBuild("expected property or ']'", "stone[");
        }
    }

    @Nested
    class TestLegacyBlock {

        @Test
        void validBlockFull() {
            var legacyBlock = assertBuilds(BlockPattern.class, "5:4");
            assertEquals(Block.ACACIA_PLANKS, legacyBlock.block());
        }

        @Test
        void validBlockPartial() {
            var legacyBlock = assertBuilds(BlockPattern.class, "1");
            assertEquals(Block.STONE, legacyBlock.block());
        }
    }

    @Nested
    class TestWeightedList {
        @Test
        void testSingleEntry() {
            // Parser cannot create this so its mostly a sanity check
            var tree = new PatternTree.WeightedList(-1, List.of(new PatternTree.LegacyBlock(0, 1, -1, 1, 0)));
            var pattern = assertInstanceOf(RandomPatternPattern.class, assertDoesNotThrow(() -> tree.into(TerraformRegistry.EMPTY)));
            assertEquals(1, pattern.total());
            assertEquals(1, pattern.entries().size());
            var child = assertInstanceOf(BlockPattern.class, pattern.entries().get(0).pattern());
            assertEquals(Block.STONE, child.block());
        }

        @Test
        void testSingleEntryTrailing() {
            assertDoesNotBuild("expected pattern", "7%stone,");
        }

        @Test
        void testMultiEntryNoWeights() {
            var pattern = assertBuilds(RandomPatternPattern.class, "stone,dirt,glass");
            assertEquals(3, pattern.total());
            assertEquals(3, pattern.entries().size());

            var child1 = pattern.entries().get(0);
            assertEquals(1, child1.weight());
            assertEquals(Block.STONE, assertInstanceOf(BlockPattern.class, child1.pattern()).block());

            var child2 = pattern.entries().get(1);
            assertEquals(1, child2.weight());
            assertEquals(Block.DIRT, assertInstanceOf(BlockPattern.class, child2.pattern()).block());

            var child3 = pattern.entries().get(2);
            assertEquals(1, child3.weight());
            assertEquals(Block.GLASS, assertInstanceOf(BlockPattern.class, child3.pattern()).block());
        }

        @Test
        void testMultiEntryWeighted() {
            var pattern = assertBuilds(RandomPatternPattern.class, "7%stone,23%dirt,12%glass");
            assertEquals(42, pattern.total());
            assertEquals(3, pattern.entries().size());

            var child1 = pattern.entries().get(0);
            assertEquals(7, child1.weight());
            assertEquals(Block.STONE, assertInstanceOf(BlockPattern.class, child1.pattern()).block());

            var child2 = pattern.entries().get(1);
            assertEquals(23, child2.weight());
            assertEquals(Block.DIRT, assertInstanceOf(BlockPattern.class, child2.pattern()).block());

            var child3 = pattern.entries().get(2);
            assertEquals(12, child3.weight());
            assertEquals(Block.GLASS, assertInstanceOf(BlockPattern.class, child3.pattern()).block());
        }

        @Test
        void testMissingChildTopLevel() {
            assertDoesNotBuild("expected pattern", "7%");
        }

        @Test
        void testMissingSecondChildLevel() {
            assertDoesNotBuild("expected pattern", "7%stone,2%");
        }

        @Test
        void testPartialChild() {
            assertDoesNotBuild("no such block: minecraft:st", "7%st");
        }
    }

    @Nested
    class TestRandomState {
        @Test
        void testValidCase() {
            assertBuilds(RandomStatePattern.class, "*stone_stairs");
        }

        @Test
        void testOnlyStarInvalid() {
            assertDoesNotBuild("expected block", "*");
        }

        @Test
        void testInvalidBlock() {
            assertDoesNotBuild("no such block: minecraft:not_a_block", "*not_a_block");
        }
    }

    @Nested
    @EnvTest //todo very bad remove once using tf registry to get tags
    class TestTag {
        @Test
        void testValidNoStar(Env env) {
            var expected = MinecraftServer.getTagManager().getTag(Tag.BasicType.BLOCKS, "minecraft:stairs")
                    .getValues().stream().map(Block::fromNamespaceId).toList();
            var pattern = assertBuilds(TagPattern.class, "##stairs");
            assertEquals(expected, pattern.blocks());
            assertFalse(pattern.randomState());
        }

        @Test
        void testValidWithStar(Env env) {
            var pattern = assertBuilds(TagPattern.class, "##*stairs");
            assertTrue(pattern.randomState());
        }

        @Test
        void testInvalidEmptyA(Env env) {
            assertDoesNotBuild("expected tag", "##");
        }

        @Test
        void testInvalidEmptyB(Env env) {
            assertDoesNotBuild("expected tag", "##*");
        }

        @Test
        void testInvalidTagA(Env env) {
            assertDoesNotBuild("no such tag: minecraft:not_a_tag", "##not_a_tag");
        }

        @Test
        void testInvalidTagB(Env env) {
            assertDoesNotBuild("no such tag: minecraft:not_a_tag", "##*not_a_tag");
        }
    }

    @Nested
    class TestTypeStateApply {
        @Test
        void testValidBlockOnly() {
            var pattern = assertBuilds(TypeStatePattern.class, "^stone");
            assertEquals(Block.STONE.id(), pattern.blockId());
        }

        @Test
        void invalidOnlyCaret() {
            assertDoesNotBuild("expected block or properties", "^");
        }

        @Test
        void invalidEmptyPropsNoBlock() {
            assertDoesNotBuild("block id or properties are required", "^[]");
        }

        @Test
        void invalidBlock() {
            assertDoesNotBuild("no such block: minecraft:sto", "^sto");
        }
    }

    private static <T extends Pattern> @NotNull T assertBuilds(@NotNull Class<T> type, @NotNull String input) {
        var parser = new PatternParser(input);
        var tree = assertDoesNotThrow(parser::parse);
        var pattern = assertDoesNotThrow(() -> tree.into(TerraformRegistry.EMPTY));
        return assertInstanceOf(type, pattern);
    }

    private static void assertDoesNotBuild(@Nullable String expectedError, @NotNull String input) {
        var parser = new PatternParser(input);
        var tree = assertDoesNotThrow(parser::parse);
        var error = assertThrows(ParseException.class, () -> tree.into(TerraformRegistry.EMPTY));
        if (expectedError != null) {
            assertEquals(expectedError, error.getMessage());
        }
    }

}
