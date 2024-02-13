package net.hollowcube.terraform.compat.worldedit.script;

import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
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
            assertNull(blockState.namespaceId().right());
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

        @Test
        public void testSingleStatePartialKeyA() {
            var parser = new PatternParser("stone[aaa");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(9, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());
            assertEquals(5, blockState.props().openBracket());
            assertEquals(-1, blockState.props().closeBracket());
        }

        @Test
        public void testSingleStatePartialKeyB() {
            var parser = new PatternParser("stone[aaa=");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(10, blockState.end());

            assertEquals("stone", blockState.namespaceId().left());
            assertEquals(5, blockState.props().openBracket());
            assertEquals(-1, blockState.props().closeBracket());
        }

        @Test
        public void testSingleWithTrailingComma() {
            var parser = new PatternParser("stone[a=b,");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(PatternTree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(10, blockState.end());
            assertEquals(9, blockState.props().trailingComma());
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
    class WeightedList {

        @Test
        void partialWeightedSingle() {
            var parser = new PatternParser("5%");
            var tree = assertDoesNotThrow(parser::parse);
            assertInstanceOf(PatternTree.WeightedList.class, tree);
        }

        @Test
        void singleWithTrailing() {
            var parser = new PatternParser("stone,");
            var tree = assertDoesNotThrow(parser::parse);
            var weightedList = assertInstanceOf(PatternTree.WeightedList.class, tree);
            assertEquals(5, weightedList.trailingComma());
        }

        @Test
        void twoEntriesNoWeights() {
            var parser = new PatternParser("1,2");
            var tree = assertDoesNotThrow(parser::parse);
            var weightedList = assertInstanceOf(PatternTree.WeightedList.class, tree);
            assertEquals(-1, weightedList.trailingComma());
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
    class RandomState {

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

    @Nested
    @EnvTest //todo very bad, can remove once using tf registry to lookup tags
    class Tag {

        @Test
        void doubleHashOnly(Env env) {
            var tree = assertDoesNotThrow(new PatternParser("##")::parse);
            var tag = assertInstanceOf(PatternTree.Tag.class, tree);
            assertEquals(0, tag.start());
            assertEquals(2, tag.end());
            assertEquals(-1, tag.star());
            assertNull(tag.namespaceId());
        }

        @Test
        void doubleHashStar(Env env) {
            var tree = assertDoesNotThrow(new PatternParser("##*")::parse);
            var tag = assertInstanceOf(PatternTree.Tag.class, tree);
            assertEquals(0, tag.start());
            assertEquals(3, tag.end());
            assertEquals(2, tag.star());
            assertNull(tag.namespaceId());
        }

        @Test
        void doubleHashStarNamespace(Env env) {
            var tree = assertDoesNotThrow(new PatternParser("##*ston")::parse);
            var tag = assertInstanceOf(PatternTree.Tag.class, tree);
            assertEquals(0, tag.start());
            assertEquals(7, tag.end());
            assertEquals(2, tag.star());
            assertNotNull(tag.namespaceId());
            assertEquals("ston", tag.namespaceId().left());
        }

    }

    @Nested
    class TypeStateApply {

        @Test
        void onlyCaret() {
            var tree = assertDoesNotThrow(new PatternParser("^")::parse);
            var typeState = assertInstanceOf(PatternTree.TypeStateApply.class, tree);
            assertEquals(0, typeState.start());
            assertEquals(1, typeState.end());
            assertNull(typeState.namespaceId());
            assertNull(typeState.props());
        }

        @Test
        void namespaceOnly() {
            var tree = assertDoesNotThrow(new PatternParser("^sto")::parse);
            var typeState = assertInstanceOf(PatternTree.TypeStateApply.class, tree);
            assertEquals(0, typeState.start());
            assertEquals(4, typeState.end());

            assertNotNull(typeState.namespaceId());
            assertEquals("sto", typeState.namespaceId().left());

            assertNull(typeState.props());
        }

        @Test
        void propertiesOnly() {
            var tree = assertDoesNotThrow(new PatternParser("^[facing=east]")::parse);
            var typeState = assertInstanceOf(PatternTree.TypeStateApply.class, tree);
            assertEquals(0, typeState.start());
            assertEquals(14, typeState.end());
            assertNull(typeState.namespaceId());

            // Not much needed here because its already tested extensively elsewhere (its reused)
            assertNotNull(typeState.props());
            assertEquals(1, typeState.props().size());
        }

        @Test
        void fullSample() {
            var tree = assertDoesNotThrow(new PatternParser("^minecraft:stone[facing=east]")::parse);
            var typeState = assertInstanceOf(PatternTree.TypeStateApply.class, tree);
            assertEquals(0, typeState.start());
            assertEquals(29, typeState.end());

            assertNotNull(typeState.namespaceId());
            assertEquals("minecraft", typeState.namespaceId().left());
            assertEquals("stone", typeState.namespaceId().right());

            // Not much needed here because its already tested extensively elsewhere (its reused)
            assertNotNull(typeState.props());
            assertEquals(1, typeState.props().size());
        }

        @Test
        void ageRegression() {
            var tree = assertDoesNotThrow(new PatternParser("^[age=12")::parse);
            var typeState = assertInstanceOf(PatternTree.TypeStateApply.class, tree);
            assertEquals(0, typeState.start());
            assertEquals(8, typeState.end());

            assertNotNull(typeState.props());
            assertEquals(1, typeState.props().size());
            var prop = typeState.props().get(0);
            assertEquals("age", prop.key());
            assertEquals("12", prop.value());
        }
    }

}
