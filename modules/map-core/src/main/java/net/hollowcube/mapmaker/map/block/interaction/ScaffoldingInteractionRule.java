package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.common.util.PlayerUtil;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class ScaffoldingInteractionRule implements BlockInteractionRule {
    public static final ScaffoldingInteractionRule INSTANCE = new ScaffoldingInteractionRule();

    // This is registered as an item interaction rule, i'm not sure it makes any difference at all but item is
    // more selective i guess. The implementation confirms that both the block & item are scaffolding, so it
    // doesn't really matter and could be changed to block without issue.

    private static final int MAX_PLACE_DISTANCE = 50;
    private static final int VANILLA_PLACE_DISTANCE = 7 * 7;
    private static final int SCAFFOLDING_BLOCK = Block.SCAFFOLDING.id();
    private static final int SCAFFOLDING_ITEM = Material.SCAFFOLDING.id();

    private ScaffoldingInteractionRule() {
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        if (interaction.item().material().id() != SCAFFOLDING_ITEM) return false;

        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);
        if (block.id() != SCAFFOLDING_BLOCK) return false;

        var worldBorder = interaction.instance().getWorldBorder();

        var startPosition = blockPosition;
        var blockFace = interaction.blockFace();
        if (blockFace == BlockFace.TOP) {
            // Interacting with the top face extends the scaffolding out in the direction the player is looking.

            var placeFace = BlockFace.fromYaw(interaction.player().getPosition().yaw());
            // Add 2 because this is triggering before the block, and the block position is exactly on so need to move in from the wb.
            for (int i = 0; i < MAX_PLACE_DISTANCE; i++) {
                blockPosition = blockPosition.relative(placeFace);
                if (!worldBorder.inBounds(blockPosition)) break;

                block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);
                if (block.id() == SCAFFOLDING_BLOCK) continue;
                if (!block.isAir()) break;

                // We found a non-scaffolding block. Place one there.
                interaction.instance().placeBlock(new BlockHandler.PlayerPlacement(
                        Block.SCAFFOLDING, interaction.instance(), blockPosition,
                        interaction.player(), interaction.hand(), placeFace.getOppositeFace(),
                        0f, 0f, 0f
                ));

                // Vanilla only allows placement up to 7 blocks away so will stop playing the sound after that.
                // We play the sound for cases where the player is placing scaffolding further than vanilla allows.
                if (startPosition.distanceSquared(blockPosition) > VANILLA_PLACE_DISTANCE) {
                    var player = interaction.player();
                    player.playSound(Sound.sound(SoundEvent.BLOCK_SCAFFOLDING_PLACE, Sound.Source.BLOCK, 1f, 1f),
                            blockPosition.x(), blockPosition.y(), blockPosition.z());
                    PlayerUtil.swing(player, interaction.hand(), true);
                }

                break;
            }
        } else {
            // Interacting with any other face adds to the top of the scaffolding stack.
            var worldHeight = interaction.instance().getCachedDimensionType().maxY();
            while (blockPosition.y() < worldHeight) {
                blockPosition = blockPosition.add(0, 1, 0);
                block = interaction.getBlock(blockPosition, Block.Getter.Condition.TYPE);
                if (block.id() == SCAFFOLDING_BLOCK) continue;
                if (!block.isAir()) break;

                // We found a non-scaffolding block. Place one there as a player placement because this is the real place position.
                interaction.instance().placeBlock(new BlockHandler.PlayerPlacement(
                        Block.SCAFFOLDING, interaction.instance(), blockPosition,
                        interaction.player(), interaction.hand(), BlockFace.TOP,
                        0f, 0f, 0f
                ));
                break;
            }
        }

        return true;
    }

}
