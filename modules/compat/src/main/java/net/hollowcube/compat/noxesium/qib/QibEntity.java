package net.hollowcube.compat.noxesium.qib;

import com.noxcrew.noxesium.api.protocol.NoxesiumFeature;
import net.hollowcube.compat.noxesium.NoxesiumAPI;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class QibEntity extends Entity {

    public QibEntity(@NotNull UUID uuid) {
        super(EntityType.INTERACTION, uuid);

        this.updateViewableRule(player -> NoxesiumAPI.canUseFeature(player, NoxesiumFeature.STABLE_CLIENT_QIBS));
    }

    public QibEntity() {
        this(UUID.randomUUID());
    }
}
