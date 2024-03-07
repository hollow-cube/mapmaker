package net.hollowcube.mapmaker.map.block.interaction;

import net.hollowcube.mapmaker.map.item.ItemTags;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

// Vanilla note: We do not require a book, it simply toggles between the filled and unfilled state.
public class ChiseledBookshelfInteractionRule implements BlockInteractionRule {
    public static final ChiseledBookshelfInteractionRule INSTANCE = new ChiseledBookshelfInteractionRule();

    private static final SoundEvent INSERT_SOUND = SoundEvent.BLOCK_CHISELED_BOOKSHELF_INSERT;
    private static final SoundEvent PICKUP_SOUND = SoundEvent.BLOCK_CHISELED_BOOKSHELF_PICKUP;

    private ChiseledBookshelfInteractionRule() {
    }

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var clickFace = interaction.blockFace();
        if (clickFace == null) return false;

        var material = interaction.item().material();
        if (material.id() != Material.AIR.id() && !ItemTags.BOOKSHELF_BOOKS.contains(material.namespace()))
            return false;
        
        var block = interaction.getBlock(interaction.blockPosition());
        var blockFace = BlockFace.valueOf(block.getProperty("facing").toUpperCase(Locale.ROOT));
        if (clickFace != blockFace) return false;

        var cursorPosition = Objects.requireNonNullElse(interaction.cursorPosition(), Vec.ZERO);
        var clickedProperty = String.format("slot_%d_occupied", getClickedSlot(clickFace, cursorPosition));

        var newProperty = "false".equals(block.getProperty(clickedProperty));
        var newBlock = block.withProperty(clickedProperty, String.valueOf(newProperty));
        interaction.setBlock(interaction.blockPosition(), newBlock);
        interaction.playBlockSound(newProperty ? INSERT_SOUND : PICKUP_SOUND, 1f, 1f);

        return true;
    }

    @Override
    public @NotNull SneakState sneakState() {
        return SneakState.NOT_SNEAKING_OR_EMPTY_HAND;
    }

    private int getClickedSlot(@NotNull BlockFace blockFace, @NotNull Point cursorPosition) {
        double x = switch (blockFace) {
            case SOUTH -> cursorPosition.x();
            case NORTH -> 1.0 - cursorPosition.x();
            case WEST -> cursorPosition.z();
            case EAST -> 1.0 - cursorPosition.z();
            default -> 0.0;
        };
        return (int) (x * 3.0) + (cursorPosition.y() > 0.5 ? 0 : 3);
    }
}
