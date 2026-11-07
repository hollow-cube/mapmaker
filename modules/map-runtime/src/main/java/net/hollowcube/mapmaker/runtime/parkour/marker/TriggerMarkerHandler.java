package net.hollowcube.mapmaker.runtime.parkour.marker;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandler;
import net.hollowcube.mapmaker.map.util.debug.WireColor;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNode;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNodeData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

public class TriggerMarkerHandler extends ObjectEntityHandler {
    public static final String ID = "mapmaker:trigger";
    public static final Tag<TriggerNodeData> DATA_TAG = TriggerNodeData.TAG.path("data");

    private @Nullable TriggerNode trigger;

    public TriggerMarkerHandler(MarkerEntity entity) {
        super(ID, entity);

        onDataChange(null); // init
    }

    @Override
    public void onDataChange(@Nullable Player player) {
        updateTrigger(entity.getPosition());
    }

    @Override
    public void onPositionChange(Pos newPosition) {
        updateTrigger(newPosition);
    }

    @Override
    public void onPlayerEnter(Player player) {
        var world = MapWorld.forPlayer(player);
        if (trigger != null && world != null) trigger.onEnter(world, player);
    }

    @Override
    public void onPlayerExit(Player player) {
        var world = MapWorld.forPlayer(player);
        if (trigger != null && world != null) trigger.onExit(world, player);
    }

    @Override
    public @Nullable WireColor debugColor() {
        return trigger != null ? trigger.debugColor() : null;
    }

    private void updateTrigger(Pos position) {
        this.trigger = new TriggerNode(
            entity.getUuid().toString(),
            position,
            entity.getTag(DATA_TAG)
        );
    }
}
