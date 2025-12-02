package net.hollowcube.mapmaker.hub.feature.conveyer;

import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.EntityPositionSyncPacket;

import java.util.function.Consumer;

public class ConveyerItemModel extends NpcItemModel {

    public ConveyerItemModel() {
        super();
    }

    public void sync(Consumer<ConveyerItemModel> consumer) {
        this.sendPacketToViewersAndSelf(new BundlePacket());
        this.getEntityMeta().setNotifyAboutChanges(false);
        consumer.accept(this);
        final var pos = this.position;
        final var delta = pos.sub(lastSyncedPosition);
        this.sendPacketToViewersAndSelf(new EntityPositionSyncPacket(getEntityId(), pos, delta, pos.yaw(), pos.pitch(), isOnGround()));
        this.lastSyncedPosition = pos;
        this.getEntityMeta().setNotifyAboutChanges(true);
        this.sendPacketToViewersAndSelf(new BundlePacket());
    }

    public void setDefaultMeta() {
        var meta = this.getEntityMeta();
        meta.setNotifyAboutChanges(false);

        meta.setPosRotInterpolationDuration(1);
        meta.setTransformationInterpolationDuration(1);
        meta.setScale(new Vec(4.5, 4.5, 4.5));
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.THIRDPERSON_RIGHT_HAND);

        meta.setNotifyAboutChanges(true);
    }
}
