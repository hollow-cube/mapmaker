package net.hollowcube.compat.noxesium.qib;

import com.noxcrew.noxesium.api.protocol.NoxesiumFeature;
import com.noxcrew.noxesium.api.qib.QibDefinition;
import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeEntityRulesPacket;
import net.hollowcube.compat.noxesium.packets.ClientboundResetEntityRulesPacket;
import net.hollowcube.compat.noxesium.rules.NoxesiumEntityRules;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class QibEntity extends Entity {

    private QibDefinition behavior = null;

    public QibEntity(@NotNull UUID uuid) {
        super(EntityType.INTERACTION, uuid);

        this.setNoGravity(true);
        this.collidesWithEntities = false;

        this.updateViewableRule(player -> NoxesiumAPI.canUseFeature(player, NoxesiumFeature.STABLE_CLIENT_QIBS));
    }

    public QibEntity() {
        this(UUID.randomUUID());
    }

    @Override
    public @NotNull InteractionMeta getEntityMeta() {
        return (InteractionMeta) super.getEntityMeta();
    }

    @Override
    public void tick(long time) {
        // Intentionally do nothing
    }

    @Override
    public void setBoundingBox(BoundingBox boundingBox) {
        super.setBoundingBox(boundingBox);

        InteractionMeta meta = getEntityMeta();
        meta.setWidth((float) boundingBox.width());
        meta.setHeight((float) boundingBox.height());

        if (this.isActive()) {
            ClientboundChangeEntityRulesPacket.qibDepth(this, boundingBox.depth()).sendToViewers(this);
        }
    }

    @Override
    public CompletableFuture<Void> setInstance(@NotNull Instance instance, @NotNull Pos spawnPosition) {
        if (this.behavior != null) {
            QibDefinitionManager.set(instance, this.getUuid().toString(), this.behavior);
        }
        return super.setInstance(instance, spawnPosition);
    }

    public void setBehavior(@Nullable QibDefinition behavior) {
        this.behavior = behavior;
        if (this.behavior == null) {
            QibDefinitionManager.remove(instance, this.getUuid().toString());
        } else {
            QibDefinitionManager.set(instance, this.getUuid().toString(), this.behavior);
        }

        if (this.isActive()) {
            getBehaviorPacket().sendToViewers(this);
        }
    }

    private ClientboundModPacket<?> getBehaviorPacket() {
        if (this.behavior == null) {
            return ClientboundResetEntityRulesPacket.of(this, NoxesiumEntityRules.QIB_BEHAVIOR);
        } else {
            return ClientboundChangeEntityRulesPacket.qibBehaviour(this, this.getUuid().toString());
        }
    }

    @Override
    @SuppressWarnings("UnstableApiUsage") // Need to override to properly send extra Noxesium entity data
    public void updateNewViewer(@NotNull Player player) {
        super.updateNewViewer(player);
        ClientboundChangeEntityRulesPacket.qibDepth(this, this.boundingBox.depth()).send(player);
        getBehaviorPacket().send(player);
    }

    @Override
    protected void despawn() {
        super.despawn();
        QibDefinitionManager.remove(instance, this.getUuid().toString());
    }
}
