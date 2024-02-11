package net.hollowcube.terraform.compat.worldedit.script;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WEPatternParseTest {

    @Nested
    class TestBlockState {
        @Test
        public void testSingleState() {
            var parser = new PatternParser("stone");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(5, blockState.end());
            assertEquals("stone", blockState.namespaceId().left());
            assertEquals(-1, blockState.props().openBracket());
            assertEquals(-1, blockState.props().closeBracket());
            assertEquals(0, blockState.props().size());
        }

        @Test
        public void testSingleStateWithEmptyBrackets() {
            var parser = new PatternParser("stone[]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(7, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());
            assertEquals(5, blockState.props().openBracket());
            assertEquals(6, blockState.props().closeBracket());
        }

        @Test
        public void testSingleStateFullId() {
            var parser = new PatternParser("minecraft:stone[]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(17, blockState.end());

            assertEquals("minecraft", blockState.namespaceId().left());
            assertEquals(15, blockState.props().openBracket());
            assertEquals("stone", blockState.namespaceId().right());

            assertEquals(15, blockState.props().openBracket());
            assertEquals(16, blockState.props().closeBracket());
        }

        @Test
        public void testSingleStateOnlyOpenBracket() {
            var parser = new PatternParser("stone[");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(6, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());
            assertEquals(5, blockState.props().openBracket());
            assertEquals(-1, blockState.props().closeBracket());
        }

        @Test
        public void testSingleStateWithProp() {
            var parser = new PatternParser("stone[facing=north]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(19, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());

            assertEquals(5, blockState.props().openBracket());
            assertEquals(18, blockState.props().closeBracket());
            assertEquals(1, blockState.props().size());

            var prop = blockState.props().get(0);
            assertEquals(6, prop.start());
            assertEquals(18, prop.end());
            assertEquals("facing", prop.key());
            assertEquals("north", prop.value());
        }

        @Test
        public void testPartialState() {
            var parser = new PatternParser("stone[facing=]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(14, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());

            assertEquals(5, blockState.props().openBracket());
            assertEquals(13, blockState.props().closeBracket());
            assertEquals(1, blockState.props().size());
            var prop = blockState.props().get(0);
            assertEquals(6, prop.start());
            assertEquals(13, prop.end());
            assertEquals("facing", prop.key());
            assertNull(prop.value());
        }

        @Test
        public void testSingleStateWithProps() {
            var parser = new PatternParser("stone[facing=north,a=b]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(23, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());

            assertEquals(5, blockState.props().openBracket());
            assertEquals(22, blockState.props().closeBracket());
            assertEquals(2, blockState.props().size());

            var prop1 = blockState.props().get(0);
            assertEquals(6, prop1.start());
            assertEquals(18, prop1.end());
            assertEquals("facing", prop1.key());
            assertEquals("north", prop1.value());

            var prop2 = blockState.props().get(1);
            assertEquals(19, prop2.start());
            assertEquals(22, prop2.end());
            assertEquals("a", prop2.key());
            assertEquals("b", prop2.value());
        }

        @Test
        public void testSingleStateTrailingComma() {
            var parser = new PatternParser("stone[facing=north,]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(20, blockState.end());

            var nsid = blockState.namespaceId();
            assertEquals(0, nsid.start());
            assertEquals(5, nsid.end());
            assertEquals(-1, nsid.colon());
            assertEquals("stone", nsid.left());

            assertEquals(5, blockState.props().openBracket());
            assertEquals(19, blockState.props().closeBracket());
            assertEquals(1, blockState.props().size());
            var prop1 = blockState.props().get(0);
            assertEquals(6, prop1.start());
            assertEquals(18, prop1.end());
            assertEquals("facing", prop1.key());
            assertEquals("north", prop1.value());
        }
    }

    @Nested
    class LegacyBlock {

        @Test
        void testOnlyId() {
            var parser = new PatternParser("42");
            var tree = assertDoesNotThrow(parser::parse);
            var legacyBlock = assertInstanceOf(PatternTree.LegacyBlock.class, tree);
            assertEquals(0, legacyBlock.start());
            assertEquals(2, legacyBlock.end());
            assertEquals(-1, legacyBlock.colon());
            assertEquals(42, legacyBlock.id());
            assertEquals(-1, legacyBlock.data());
        }

        @Test
        void testIdColonNoData() {
            var parser = new PatternParser("42:");
            var tree = assertDoesNotThrow(parser::parse);
            var legacyBlock = assertInstanceOf(PatternTree.LegacyBlock.class, tree);
            assertEquals(0, legacyBlock.start());
            assertEquals(3, legacyBlock.end());
            assertEquals(2, legacyBlock.colon());
            assertEquals(42, legacyBlock.id());
            assertEquals(-1, legacyBlock.data());
        }

        @Test
        void testIdAndData() {
            var parser = new PatternParser("42:24");
            var tree = assertDoesNotThrow(parser::parse);
            var legacyBlock = assertInstanceOf(PatternTree.LegacyBlock.class, tree);
            assertEquals(0, legacyBlock.start());
            assertEquals(5, legacyBlock.end());
            assertEquals(2, legacyBlock.colon());
            assertEquals(42, legacyBlock.id());
            assertEquals(24, legacyBlock.data());
        }
    }

    @Nested
    public class WeightedList {

        @Test
        void twoEntriesNoWeights() {
            var parser = new PatternParser("1,2");
            var tree = assertDoesNotThrow(parser::parse);
            var weightedList = assertInstanceOf(PatternTree.WeightedList.class, tree);
            assertEquals(0, weightedList.start());
            assertEquals(3, weightedList.end());
            assertEquals(2, weightedList.entries().size());

            var first = assertInstanceOf(PatternTree.LegacyBlock.class, weightedList.entries().get(0));
            assertEquals(1, first.id());
            var second = assertInstanceOf(PatternTree.LegacyBlock.class, weightedList.entries().get(1));
            assertEquals(2, second.id());
        }

        @Test
        void twoEntriesWeighted() {
            var parser = new PatternParser("10%1,20%2");
            var tree = assertDoesNotThrow(parser::parse);
            var weightedList = assertInstanceOf(PatternTree.WeightedList.class, tree);
            assertEquals(0, weightedList.start());
            assertEquals(9, weightedList.end());
            assertEquals(2, weightedList.entries().size());

            var firstWeighted = assertInstanceOf(PatternTree.Weighted.class, weightedList.entries().get(0));
            var first = assertInstanceOf(PatternTree.LegacyBlock.class, firstWeighted.pattern());
            assertEquals(1, first.id());

            var secondWeighted = assertInstanceOf(PatternTree.Weighted.class, weightedList.entries().get(1));
            var second = assertInstanceOf(PatternTree.LegacyBlock.class, secondWeighted.pattern());
            assertEquals(2, second.id());
        }
    }

    @Nested
    public class RandomState {

        @Test
        void onlyStar() {
            var parser = new PatternParser("*");
            var tree = assertDoesNotThrow(parser::parse);
            var randomState = assertInstanceOf(PatternTree.RandomState.class, tree);
            assertEquals(0, randomState.start());
            assertEquals(1, randomState.end());
            assertNull(randomState.namespaceId());
        }

        @Test
        void fullPattern() {
            var parser = new PatternParser("*stone_stairs");
            var tree = assertDoesNotThrow(parser::parse);
            var randomState = assertInstanceOf(PatternTree.RandomState.class, tree);
            assertEquals(0, randomState.start());
            assertEquals(13, randomState.end());
            assertNotNull(randomState.namespaceId());
            assertEquals("stone_stairs", randomState.namespaceId().left());
        }

    }

}
