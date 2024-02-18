package net.hollowcube.mapmaker.map.entity.impl;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.other.EndCrystalMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class EndCrystalEntity extends MapEntity {
    public EndCrystalEntity(@NotNull UUID uuid) {
        super(EntityType.END_CRYSTAL, uuid);
    }

    @Override
    public @NotNull EndCrystalMeta getEntityMeta() {
        return (EndCrystalMeta) super.getEntityMeta();
    }

    @Override
    public void onBuildLeftClick(@NotNull MapWorld world, @NotNull Player player) {
        remove();
    }
}
