package net.hollowcube.map.block.placement;

import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class DoorPlacementRule extends FacingHorizontalPlacementRule {
    public DoorPlacementRule(@NotNull Block block) {
        super(block, false);
    }

    @Override
    public @Nullable Block blockPlace(@NotNull PlacementState placementState) {
        if (placementState.instance().getBlock(placementState.placePosition().add(0, 1, 0)).isAir()) {
            // We can place above, proceed
            Block toPlace = Objects.requireNonNull(super.blockPlace(placementState), "unreachable");
            // Check if we should have inverted hinge - based on side of the block we placed on, or, if there is another door block to our left or right
            boolean isLeft = true;

            // Cursor position ranges for 0-1 for x and z, using that to determine which part we need to care about
            switch (block.getProperty("facing")) {
                case "north" -> {
                    if (isDoor(placementState.instance().getBlock(placementState.placePosition().add(-1, 0, 0))) || placementState.cursorPosition().x() > 0.5) {
                        isLeft = false;
                    }
                }
                case "south" -> {
                    if (isDoor(placementState.instance().getBlock(placementState.placePosition().add(1, 0, 0))) || placementState.cursorPosition().x() < 0.5) {
                        isLeft = false;
                    }
                }
                case "east" -> {
                    if (isDoor(placementState.instance().getBlock(placementState.placePosition().add(0, 0, -1))) || placementState.cursorPosition().z() > 0.5) {
                        isLeft = false;
                    }
                }
                case "west" -> {
                    if (isDoor(placementState.instance().getBlock(placementState.placePosition().add(0, 0, 1))) || placementState.cursorPosition().z() < 0.5) {
                        isLeft = false;
                    }
                }
            }
            toPlace = toPlace.withProperty("hinge", isLeft ? "left" : "right");
            // TODO: We shouldn't be doing this and should come up with a different way of setting blocks upon placement
            if (placementState.instance() instanceof Instance currInstance) {
                currInstance.setBlock(placementState.placePosition().add(0, 1, 0), toPlace.withProperty("half", "upper"));
            }
            return toPlace;
        } else {
            return null;
        }
    }

    private boolean isDoor(Block toCheck) {
        return BlockTags.MINECRAFT_DOORS.contains(toCheck.namespace());
    }
}
