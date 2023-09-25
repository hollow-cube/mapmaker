package net.hollowcube.terraform.mask.script;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParser {

    @Nested
    class TestBlockState {
        @Test
        public void testSingleState() {
            var parser = new Parser("stone");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(5, blockState.end());
            assertEquals(-1, blockState.openBracket());
            assertEquals(-1, blockState.closeBracket());
            assertEquals("stone", blockState.block());
            assertNull(blockState.props());
        }

        @Test
        public void testSingleStateWithEmptyBrackets() {
            var parser = new Parser("stone[]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(7, blockState.end());
            assertEquals(5, blockState.openBracket());
            assertEquals(6, blockState.closeBracket());
            assertEquals("stone", blockState.block());
            assertNull(blockState.props());
        }

        @Test
        public void testSingleStateOnlyOpenBracket() {
            var parser = new Parser("stone[");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(6, blockState.end());
            assertEquals(5, blockState.openBracket());
            assertEquals(-1, blockState.closeBracket());
            assertEquals("stone", blockState.block());
            assertNull(blockState.props());
        }

        @Test
        public void testSingleStateWithProp() {
            var parser = new Parser("stone[facing=north]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(19, blockState.end());
            assertEquals(5, blockState.openBracket());
            assertEquals(18, blockState.closeBracket());
            assertEquals("stone", blockState.block());
            assertEquals(1, blockState.props().size());
            var prop = blockState.props().get(0);
            assertEquals(6, prop.start());
            assertEquals(18, prop.end());
            assertEquals("facing", prop.key());
            assertEquals("north", prop.value());
        }

        @Test
        public void testPartialState() {
            var parser = new Parser("stone[facing=]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(14, blockState.end());
            assertEquals(5, blockState.openBracket());
            assertEquals(13, blockState.closeBracket());
            assertEquals("stone", blockState.block());
            assertEquals(1, blockState.props().size());
            var prop = blockState.props().get(0);
            assertEquals(6, prop.start());
            assertEquals(13, prop.end());
            assertEquals("facing", prop.key());
            assertNull(prop.value());
        }

        @Test
        public void testSingleStateWithProps() {
            var parser = new Parser("stone[facing=north,a=b]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(23, blockState.end());
            assertEquals(5, blockState.openBracket());
            assertEquals(22, blockState.closeBracket());
            assertEquals("stone", blockState.block());
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
            var parser = new Parser("stone[facing=north,]");
            var tree = assertDoesNotThrow(parser::parse);
            var blockState = assertInstanceOf(Tree.BlockState.class, tree);
            assertEquals(0, blockState.start());
            assertEquals(20, blockState.end());
            assertEquals(5, blockState.openBracket());
            assertEquals(19, blockState.closeBracket());
            assertEquals("stone", blockState.block());
            assertEquals(1, blockState.props().size());
            var prop1 = blockState.props().get(0);
            assertEquals(6, prop1.start());
            assertEquals(18, prop1.end());
            assertEquals("facing", prop1.key());
            assertEquals("north", prop1.value());
        }
    }

    @Nested
    class TestNot {

        @Test
        public void testNotNoFollow() {
            var parser = new Parser("!");
            var tree = assertDoesNotThrow(parser::parse);
            var not = assertInstanceOf(Tree.Prefix.class, tree);
            assertEquals(0, not.start());
            assertEquals(1, not.end());
            assertEquals(Tree.Prefix.Type.NOT, not.type());
            assertNull(not.child());
        }

        @Test
        public void testWithChild() {
            var parser = new Parser("!stone");
            var tree = assertDoesNotThrow(parser::parse);
            var not = assertInstanceOf(Tree.Prefix.class, tree);
            assertEquals(0, not.start());
            assertEquals(6, not.end());
            assertEquals(Tree.Prefix.Type.NOT, not.type());
            var child = assertInstanceOf(Tree.BlockState.class, not.child());
            assertEquals(1, child.start());
            assertEquals(6, child.end());
            assertEquals(-1, child.openBracket());
            assertEquals(-1, child.closeBracket());
            assertEquals("stone", child.block());
        }

    }

    @Nested
    class TestInfix {

        @Test
        public void testInfixNoFollow() {
            var parser = new Parser("stone|");
            var tree = assertDoesNotThrow(parser::parse);
            var or = assertInstanceOf(Tree.Infix.class, tree);
            assertEquals(0, or.start());
            assertEquals(6, or.end());
            assertEquals(Tree.Infix.Type.OR, or.type());
            var lhs = assertInstanceOf(Tree.BlockState.class, or.lhs());
            assertEquals("stone", lhs.block());
            assertNull(or.rhs());
        }

        @Test
        public void testInfixHappyCase() {
            var parser = new Parser("stone|dirt");
            var tree = assertDoesNotThrow(parser::parse);
            var or = assertInstanceOf(Tree.Infix.class, tree);
            assertEquals(0, or.start());
            assertEquals(10, or.end());
            assertEquals(Tree.Infix.Type.OR, or.type());
            var lhs = assertInstanceOf(Tree.BlockState.class, or.lhs());
            assertEquals("stone", lhs.block());
            var rhs = assertInstanceOf(Tree.BlockState.class, or.rhs());
            assertEquals("dirt", rhs.block());
        }

        @Test
        public void testInfixChain() {
            var parser = new Parser("stone|dirt|grass");
            var tree = assertDoesNotThrow(parser::parse);
            var or = assertInstanceOf(Tree.Infix.class, tree);
            assertEquals(0, or.start());
            assertEquals(16, or.end());
            assertEquals(Tree.Infix.Type.OR, or.type());
            var lhs = assertInstanceOf(Tree.Infix.class, or.lhs());
            assertEquals(0, lhs.start());
            assertEquals(10, lhs.end());
            assertEquals("stone", assertInstanceOf(Tree.BlockState.class, lhs.lhs()).block());
            assertEquals("dirt", assertInstanceOf(Tree.BlockState.class, lhs.rhs()).block());
            assertEquals("grass", assertInstanceOf(Tree.BlockState.class, or.rhs()).block());
        }

        @Test
        public void testInfixInvalidOperator() {
            var parser = new Parser("stone#dirt");
            var tree = assertDoesNotThrow(parser::parse);
            var or = assertInstanceOf(Tree.Infix.class, tree);
            assertEquals(0, or.start());
            assertEquals(10, or.end());
            assertEquals(Tree.Infix.Type.ERROR, or.type());
        }
    }

    @Nested
    class TestNamed {

        @Test
        public void testHashAlone() {
            var parser = new Parser("#");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(1, named.end());
            assertNull(named.name());
        }

        @Test
        public void testNamedNoArgs() {
            var parser = new Parser("#foo");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(4, named.end());
            assertEquals("foo", named.name());
        }

        @Test
        public void testNamedOpenBracketOnly() {
            var parser = new Parser("#foo[");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(5, named.end());
            assertEquals(4, named.openBracket());
            assertEquals("foo", named.name());
        }

        @Test
        public void testNamedOpenCloseBracket() {
            var parser = new Parser("#foo[]");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(6, named.end());
            assertEquals(4, named.openBracket());
            assertEquals(5, named.closeBracket());
            assertEquals("foo", named.name());
        }

        @Test
        public void testNamedSinglePositionArg() {
            var parser = new Parser("#foo[1]");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(7, named.end());
            assertEquals(4, named.openBracket());
            assertEquals(6, named.closeBracket());
            assertEquals("foo", named.name());
            assertEquals(1, named.args().size());
            var arg1 = named.args().get(0);
            assertEquals(5, arg1.start());
            assertEquals(6, arg1.end());
            var arg1Num = assertInstanceOf(Tree.Number.class, arg1.value());
            assertEquals(1, arg1Num.value());
        }

        @Test
        public void testNamedMultiPositionArg() {
            var parser = new Parser("#foo[1,2,3]");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(11, named.end());
            assertEquals(4, named.openBracket());
            assertEquals(10, named.closeBracket());
            assertEquals("foo", named.name());
            assertEquals(3, named.args().size());
            for (int i = 1; i <= 3; i++) {
                var arg1 = named.args().get(i - 1);
                assertEquals(5 + ((i - 1) * 2), arg1.start());
                assertEquals(6 + ((i - 1) * 2), arg1.end());
                var arg1Num = assertInstanceOf(Tree.Number.class, arg1.value());
                assertEquals(i, arg1Num.value());
            }
        }

        @Test
        public void testNamedSinglePositionArgNamed() {
            var parser = new Parser("#foo[#bar]");
            var tree = assertDoesNotThrow(parser::parse);
            var named = assertInstanceOf(Tree.Named.class, tree);
            assertEquals(0, named.start());
            assertEquals(10, named.end());
            assertEquals(4, named.openBracket());
            assertEquals(9, named.closeBracket());
            assertEquals("foo", named.name());
            assertEquals(1, named.args().size());
            var arg1 = named.args().get(0);
            assertEquals(5, arg1.start());
            assertEquals(9, arg1.end());
            var arg1Named = assertInstanceOf(Tree.Named.class, arg1.value());
            assertEquals("bar", arg1Named.name());
        }

    }

}
