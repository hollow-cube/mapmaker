package net.hollowcube.mapmaker.map.block.placement;

import net.hollowcube.mapmaker.map.block.PlacementRules;
import net.hollowcube.terraform.Terraform;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

@EnvTest
class TestChestPlacementRule extends AbstractPlacementRuleTest {
    private static final BlockPlacementRule RULE = new ChestPlacementRule(Block.CHEST);

    @Test
    void hello(Env env) {
        PlacementRules.init(Terraform.builder().module(Terraform.BASE_MODULE).build());
        var instance = env.createFlatInstance();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) instance.loadChunk(x, z).join();

//        RULE.blockPlace(new BlockPlacementRule.PlacementState())

        System.out.println(instance.getBlock(0, 39, 0));


        System.out.println("Hello, world!");
    }
}
