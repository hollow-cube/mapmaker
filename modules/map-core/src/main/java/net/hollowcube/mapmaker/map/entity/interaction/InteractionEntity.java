package net.hollowcube.mapmaker.map.entity.interaction;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntity;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InteractionEntity extends ObjectEntity {

    public InteractionEntity(@NotNull UUID uuid) {
        super(EntityType.INTERACTION, uuid);

        this.sendToClient = true;
    }

    @Override
    public void onRightClick(@NotNull MapWorld world, @NotNull Player player, @NotNull PlayerHand hand, @NotNull Point interactPosition) {
        if (this.handler == null) return;
        this.handler.onPlayerInteract(player);
    }
}
