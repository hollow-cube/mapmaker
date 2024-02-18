package net.hollowcube.mapmaker.map.block.interaction;

import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LecternInteractionRule implements BlockInteractionRule {
    private static final Set<Material> BOOK_LIKE = Set.of(
            Material.WRITTEN_BOOK,
            Material.WRITABLE_BOOK,
            Material.BOOK,
            Material.ENCHANTED_BOOK,
            Material.KNOWLEDGE_BOOK
    );

    @Override
    public boolean handleInteraction(@NotNull Interaction interaction) {
        var blockPosition = interaction.blockPosition();
        var block = interaction.getBlock(blockPosition);

        // If it already has a book, remove it if there is no item in hand
        if ("true".equals(block.getProperty("has_book"))) {
            if (!interaction.item().isAir()) return false;

            interaction.setBlock(interaction.blockPosition(), block.withProperty("has_book", "false"));
            return false;
        }

        // If holding a book, put it in the lectern
        if (!BOOK_LIKE.contains(interaction.item().material())) return false;
        interaction.setBlock(blockPosition, block.withProperty("has_book", "true"));
        return true;
    }
}
