package net.hollowcube.terraform.mask;

import net.hollowcube.terraform.action.edit.MockWorldView;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//todo it may be possible to merge BlockStateMask and BlockMask
// would use them like "stone" or "stone[waterlogged=true]" or "[waterlogged=true]" to just match any block with those properties.
public class TestBlockStateMask {

    private static Stream<Arguments> matchLenientSupplier() {
        return Stream.of(
                Arguments.of("no states matches all", Map.of(), Block.STONE, true),
                Arguments.of("no states matches all 2", Map.of(),
                        Block.fromStateId((short) (Block.ACACIA_STAIRS.stateId() + 3)), true),
                Arguments.of("single prop matches block without state", Map.of("waterlogged", "true"), Block.STONE, true),
                Arguments.of("single prop exact match", Map.of("waterlogged", "true"),
                        Block.STONE_STAIRS.withProperty("waterlogged", "true"), true),
                Arguments.of("single prop exact mismatch", Map.of("waterlogged", "true"),
                        Block.STONE_STAIRS.withProperty("waterlogged", "false"), false),
                Arguments.of("multi prop exact match", Map.of("waterlogged", "true", "facing", "north"),
                        Block.STONE_STAIRS.withProperties(Map.of("waterlogged", "true", "facing", "north")), true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matchLenientSupplier")
    public void testMatchLenient(String name, Map<String, String> props, Block block, boolean match) {
        var mask = new BlockStateMask(props, false);
        var result = mask.test(MockWorldView.none(), Vec.ZERO, block);
        if (match) {
            assertTrue(result, "Mask should match");
        } else {
            assertFalse(result, "Mask should not match");
        }
    }

    private static Stream<Arguments> matchStrictSupplier() {
        return Stream.of(
                Arguments.of("no states matches all", Map.of(), Block.STONE, true),
                Arguments.of("no states matches all 2", Map.of(),
                        Block.fromStateId((short) (Block.ACACIA_STAIRS.stateId() + 3)), true),
                Arguments.of("single prop mismatches block without state", Map.of("waterlogged", "true"), Block.STONE, false),
                Arguments.of("single prop exact match", Map.of("waterlogged", "true"),
                        Block.STONE_STAIRS.withProperty("waterlogged", "true"), true),
                Arguments.of("single prop exact mismatch", Map.of("waterlogged", "true"),
                        Block.STONE_STAIRS.withProperty("waterlogged", "false"), false),
                Arguments.of("multi prop exact match", Map.of("waterlogged", "true", "facing", "north"),
                        Block.STONE_STAIRS.withProperties(Map.of("waterlogged", "true", "facing", "north")), true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("matchStrictSupplier")
    public void testMatchStrict(String name, Map<String, String> props, Block block, boolean match) {
        var mask = new BlockStateMask(props, true);
        var result = mask.test(MockWorldView.none(), Vec.ZERO, block);
        if (match) {
            assertTrue(result, "Mask should match");
        } else {
            assertFalse(result, "Mask should not match");
        }
    }
}
