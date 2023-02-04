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

public class TestBlockMask {

    private static Stream<Arguments> nonMatchingBlockSupplier() {
        return Stream.of(
                Arguments.of("Different block ID", Block.AIR, Block.STONE.id(), Map.of(), false),
                Arguments.of("Missing property", Block.AIR, Block.AIR.id(), Map.of("foo", "bar"), false),
                Arguments.of("Fuzzy property match", Block.ACACIA_FENCE.withProperty("south", "false"), Block.ACACIA_FENCE.id(), Map.of(), true),
                Arguments.of("Semi-fuzzy property match", Block.OAK_DOOR.withProperties(Map.of("open", "true", "hinge", "right")),
                        Block.OAK_DOOR.id(), Map.of("open", "true"), true),
                Arguments.of("Exact property match", Block.BELL.withProperties(Map.of("attachment", "ceiling", "facing", "south", "powered", "false")),
                        Block.BELL.id(), Map.of("attachment", "ceiling", "facing", "south", "powered", "false"), true),
                Arguments.of("Exact property mis-match", Block.BELL.withProperties(Map.of("attachment", "ceiling", "facing", "south", "powered", "false")),
                        Block.BELL.id(), Map.of("attachment", "ceiling", "facing", "south", "powered", "true"), false) // powered is mismatched

        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonMatchingBlockSupplier")
    public void testNonMatchingBlock(String name, Block block, int maskBlockId, Map<String, String> maskProperties, boolean match) {
        var mask = new BlockMask(maskBlockId, maskProperties);
        var result = mask.test(MockWorldView.none(), Vec.ZERO, block);
        if (match) {
            assertTrue(result, "Expected match, but was not");
        } else {
            assertFalse(result, "Expected no match, but was");
        }
    }
}
