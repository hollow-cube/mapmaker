package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.LeashKnotMeta;

import java.util.UUID;

public class LeashKnotEntity extends MapEntity<LeashKnotMeta> {

    public LeashKnotEntity(UUID uuid) {
        super(EntityType.LEASH_KNOT, uuid);
        hasPhysics = false;
        setNoGravity(true);
    }

    @Override
    public void onBuildLeftClick(MapWorld world, Player player) {
        this.remove();
    }
}
