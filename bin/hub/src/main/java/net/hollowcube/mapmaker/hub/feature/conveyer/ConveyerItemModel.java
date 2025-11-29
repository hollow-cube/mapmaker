package net.hollowcube.mapmaker.hub.feature.conveyer;

import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
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
        this.getEntityMeta().setNotifyAboutChanges(true);
        final var pos = this.position;
        final var delta = pos.sub(lastSyncedPosition);
        this.sendPacketToViewersAndSelf(new EntityPositionSyncPacket(getEntityId(), pos, delta, pos.yaw(), pos.pitch(), isOnGround()));
        this.lastSyncedPosition = pos;
        this.sendPacketToViewersAndSelf(new BundlePacket());
    }
}
