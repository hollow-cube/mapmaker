package net.hollowcube.mapmaker.map.entity.object.builtin;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.common.util.dfu.NbtOps;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TeleportObjectHandler extends ObjectEntityHandler {

    public static final String ID = "mapmaker:teleport";

    private Pos destination;

    public TeleportObjectHandler(@NotNull InteractionEntity entity) {
        super(ID, entity);

        onDataChange(null);
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        this.destination = ExtraCodecs.POS.parse(NbtOps.INSTANCE, entity.getData().get("destination")).result().orElse(null);
    }

    @Override
    public void onPlayerInteract(@NotNull Player player) {
        if (this.destination == null) return;

        player.teleport(this.destination);
    }
}
