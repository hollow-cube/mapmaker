package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.PlacementRules;
import net.hollowcube.terraform.Terraform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.testing.util.MockBlockGetter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestWaterloggedBlockPlacementRule {

    private static Stream<Arguments> waterloggedBlockSupplier() {
        return Block.values().stream()
                .filter(block -> block.properties().containsKey("waterlogged"))
                .map(block -> Arguments.of(block.name(), block));
    }

    static {
        MinecraftServer.init();
        PlacementRules.init(Terraform.builder().module(Terraform.BASE_MODULE).build());
    }

    //todo: investigate whats going on with default-waterlogged blocks like kelp
    @ParameterizedTest(name = "{0}")
    @MethodSource("waterloggedBlockSupplier")
    void testPlaceWaterloggedBlocks(String name, Block block) {
        var rule = MinecraftServer.getBlockManager().getBlockPlacementRule(block);
        assertNotNull(rule);

        var result = rule.blockPlace(new BlockPlacementRule.PlacementState(
                MockBlockGetter.all(Block.WATER),
                block, BlockFace.NORTH, Vec.ZERO,
                null, null,
                null, false
        ));
        assertNotNull(result);
        assertEquals("true", result.getProperty("waterlogged"), "mismatch using " + rule.getClass().getSimpleName());
    }
}
