package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.map.item.ItemTags;
import net.hollowcube.terraform.util.math.DirectionUtil;
import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.component.PotDecorations;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class DecoratedPotBlockHandler implements BlockHandler {

    private static final Key ID = Key.key("minecraft:decorated_pot");
    public static final Tag<PotDecorations> SHERDS = ExtraTags.DataComponent("sherds", ItemComponent.POT_DECORATIONS)
            .defaultValue(PotDecorations.EMPTY);

    DecoratedPotBlockHandler() {
    }

    @Override
    public @NotNull Key getKey() {
        return ID;
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var player = interaction.getPlayer();
        var stack = player.getItemInHand(interaction.getHand());
        var block = interaction.getBlock();
        if (ItemTags.SHERDS.contains(stack.material().key()) || (stack.isAir() && player.isSneaking())) {

            var front = Direction.valueOf(block.getProperty("facing").toUpperCase(Locale.ROOT));
            var face = PotFace.fromDirections(front.opposite(), interaction.getBlockFace().toDirection());

            if (face == null) return false;

            var material = stack.material();

            var sherds = block.getTag(SHERDS);
            sherds = switch (face) {
                case FRONT -> new PotDecorations(sherds.back(), sherds.left(), sherds.right(), material);
                case BACK -> new PotDecorations(material, sherds.left(), sherds.right(), sherds.front());
                case LEFT -> new PotDecorations(sherds.back(), material, sherds.right(), sherds.front());
                case RIGHT -> new PotDecorations(sherds.back(), sherds.left(), material, sherds.front());
            };

            interaction.getInstance().setBlock(
                    interaction.getBlockPosition(),
                    interaction.getBlock().withTag(SHERDS, sherds)
            );
            return true;
        }
        return true;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(SHERDS);
    }

    private enum PotFace {
        FRONT, BACK, LEFT, RIGHT;

        public static PotFace fromDirections(Direction front, Direction face) {
            if (front == face) return FRONT;
            if (front == face.opposite()) return BACK;
            if (front == DirectionUtil.rotate(face, true)) return RIGHT;
            if (front == DirectionUtil.rotate(face, false)) return LEFT;
            return null;
        }
    }
}
