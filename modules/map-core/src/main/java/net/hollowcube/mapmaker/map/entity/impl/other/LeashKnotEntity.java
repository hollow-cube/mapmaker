package net.hollowcube.mapmaker.map.entity.impl.other;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.LeashKnotMeta;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class LeashKnotEntity extends MapEntity {

    public LeashKnotEntity(@NotNull UUID uuid) {
        super(EntityType.LEASH_KNOT, uuid);
        hasPhysics = false;
        setNoGravity(true);
    }

    @Override
    public @NotNull LeashKnotMeta getEntityMeta() {
        return (LeashKnotMeta) super.getEntityMeta();
    }

    @Override
    public void onBuildRightClick(@NotNull MapWorld world, @NotNull Player player, @NotNull PlayerHand hand, @NotNull Point interactPosition) {
        var material = player.getItemInHand(hand).material();
        if (material == Material.SHEARS) {
            this.remove();
        }
    }

}
