package net.hollowcube.common.extensions;

import net.minestom.server.collision.BoundingBox;
import net.minestom.server.entity.EntityPose;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

// Extension of Player that handles certain things minestom doesnt.
// TODO handle setting of swimming
public class ExtendedPlayer extends Player {

    public ExtendedPlayer(@NotNull PlayerConnection connection, @NotNull GameProfile profile) {
        super(connection, profile);
    }

    // region Bounding Box Scaling
    // Notes: This does not handle all bounding boxes due to some of them just not handling poses anyway.

    @Override
    public @NotNull BoundingBox getBoundingBox() {
        return this.getBoundingBox(this.getPose());
    }

    public BoundingBox getBoundingBox(@NotNull EntityPose pose) {
        if (pose == EntityPose.SLEEPING) return BoundingBox.fromPose(pose); // Vanilla doesnt scale sleeping box
        var box = Objects.requireNonNullElse(BoundingBox.fromPose(pose), this.boundingBox);
        var scale = this.getAttributeValue(Attribute.SCALE);
        return scale == 1.0 ? box : new BoundingBox(box.width() * scale, box.height() * scale, box.depth() * scale);
    }
    // endregion
}
