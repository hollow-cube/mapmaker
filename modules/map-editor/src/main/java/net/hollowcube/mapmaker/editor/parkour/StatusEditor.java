package net.hollowcube.mapmaker.editor.parkour;

import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityEditor;
import net.hollowcube.mapmaker.map.event.Map2PlayerBlockInteractEvent;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorView;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.hollowcube.mapmaker.runtime.parkour.block.StatusPlateBlock;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.TagHandler;

import java.util.Objects;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class StatusEditor {

    public static final ItemHandler PLATE_ITEM = new BlockItemHandler(() -> StatusPlateBlock.INSTANCE,
            Block.STONE_PRESSURE_PLATE, "status_plate");

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(Map2PlayerBlockInteractEvent.class, StatusEditor::handleBlockInteract);

    public static final ObjectEntityEditor MARKER_EDITOR = (player, entity) -> {
        var statusData = Objects.requireNonNullElseGet(entity.getTag(StatusPlateBlock.ENTITY_DATA_TAG), ActionTriggerData::new);
        var actionLocation = entity.getPosition().withY(y -> y + Objects.requireNonNullElse(entity.getMin(), Pos.ZERO).y());
        var host = Panel.open(player, new ActionEditorView(statusData.actions(), "Status"));
        host.setTag(ActionEditorView.ACTION_LOCATION, actionLocation);
        host.setTag(TeleportAction.SPC_TAG, entity);
        host.onClose(() -> {
            entity.setTag(StatusPlateBlock.ENTITY_DATA_TAG, statusData);
            entity.handleDataChange(player);
        });
        return true;
    };

    public static void editBlock(Instance instance, Point blockPosition, Block block, Consumer<ActionTriggerData> func) {
        var data = block.getTag(StatusPlateBlock.DATA_TAG);
        func.accept(data);

        var newTag = TagHandler.newHandler();
        newTag.setTag(StatusPlateBlock.DATA_TAG, data);
        instance.setBlock(blockPosition, block.withNbt(newTag.asCompound()));
    }

    private static void handleBlockInteract(Map2PlayerBlockInteractEvent event) {
        if (!(event.block().handler() instanceof StatusPlateBlock))
            return;
        event.setCancelled(true); // Always handle status plates

        var player = event.player();
        var world = event.world();
        if (world.itemRegistry().isOnCooldown(player))
            return;

        // Open checkpoint settings GUI
        var data = Objects.requireNonNullElseGet(event.block().getTag(StatusPlateBlock.DATA_TAG), ActionTriggerData::new);
        var host = Panel.open(player, new ActionEditorView(data.actions(), "Status"));
        host.setTag(ActionEditorView.ACTION_LOCATION, event.blockPosition());
        host.setTag(TeleportAction.SPC_TAG, event.blockPosition());
        host.onClose(() -> {
            var newNbt = DFU.encodeNbt(ActionTriggerData.CODEC, data);
            world.instance().setBlock(event.blockPosition(), event.block().withNbt(newNbt));
        });
    }

}
