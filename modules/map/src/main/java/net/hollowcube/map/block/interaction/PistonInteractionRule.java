package net.hollowcube.map.block.interaction;

import net.kyori.adventure.sound.Sound;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

public class PistonInteractionRule implements BlockInteractionRule {
    public static final PistonInteractionRule INSTANCE = new PistonInteractionRule();

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);
        var isExtended = Boolean.parseBoolean(block.getProperty("extended"));

        var facing = block.getProperty("facing");
        var otherPosition = blockPosition.relative(switch (facing) {
            case "up" -> BlockFace.TOP;
            case "down" -> BlockFace.BOTTOM;
            case "north" -> BlockFace.NORTH;
            case "south" -> BlockFace.SOUTH;
            case "west" -> BlockFace.WEST;
            case "east" -> BlockFace.EAST;
            default -> throw new IllegalStateException("unreachable");
        });
        if (!interaction.worldBorder().isInside(otherPosition)) return false;
        var otherBlock = interaction.getBlock(otherPosition);

        if (isExtended) {
            if (otherBlock.id() == Block.PISTON_HEAD.id() && otherBlock.getProperty("facing").equals(facing)) {
                interaction.setBlock(otherPosition, Block.AIR);
                interaction.playSound(Sound.sound(SoundEvent.BLOCK_PISTON_CONTRACT, Sound.Source.BLOCK, 0.5f, ThreadLocalRandom.current().nextFloat() * 0.25f + 0.6f), blockPosition);
            }
        } else {
            if (otherBlock.id() != Block.AIR.id()) return false;

            interaction.setBlock(otherPosition, Block.PISTON_HEAD.withProperty("facing", facing)
                    .withProperty("type", String.valueOf(block.id() == Block.STICKY_PISTON.id() ? "sticky" : "normal")));
            interaction.playSound(Sound.sound(SoundEvent.BLOCK_PISTON_EXTEND, Sound.Source.BLOCK, 0.5f, ThreadLocalRandom.current().nextFloat() * 0.25f + 0.6f), blockPosition);
        }

        interaction.setBlock(blockPosition, block.withProperty("extended", String.valueOf(!isExtended)));
        return true;
    }
}
