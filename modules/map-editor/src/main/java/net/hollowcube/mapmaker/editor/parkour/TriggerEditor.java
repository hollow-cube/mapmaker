package net.hollowcube.mapmaker.editor.parkour;

import net.hollowcube.mapmaker.map.entity.object.ObjectEntityEditor;
import net.hollowcube.mapmaker.map.event.Map2PlayerBlockInteractEvent;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorView;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.base.CoordinateAction;
import net.hollowcube.mapmaker.runtime.parkour.block.PressurePlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.marker.TriggerMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.node.*;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class TriggerEditor {

    public static final BlockItemHandler CHECKPOINT_PLATE_ITEM = plateItem(
        "checkpoint_plate",
        Block.HEAVY_WEIGHTED_PRESSURE_PLATE,
        TriggerTemplate.checkpoint()
    );
    public static final BlockItemHandler STATUS_PLATE_ITEM = plateItem(
        "status_plate",
        Block.STONE_PRESSURE_PLATE,
        TriggerTemplate.status()
    );
    public static final BlockItemHandler FINISH_PLATE_ITEM = plateItem(
        "finish_plate",
        Block.LIGHT_WEIGHTED_PRESSURE_PLATE,
        TriggerTemplate.finish()
    );

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode().addListener(
        Map2PlayerBlockInteractEvent.class,
        TriggerEditor::handleBlockInteract
    );

    public static final ObjectEntityEditor MARKER_EDITOR = (player, entity) -> {
        var node = entity.getTag(TriggerMarkerHandler.DATA_TAG);
        var type = actionType(node);
        if (type == null) return true;

        var data = node.actionData().toMutable();
        var actionLocation = entity.getPosition().withY(
            y -> y + Objects.requireNonNullElse(entity.getMin(), Pos.ZERO).y()
        );

        var host = Panel.open(player, new ActionEditorView(data, type));
        host.setTag(ActionEditorView.ACTION_LOCATION, actionLocation);
        host.setTag(CoordinateAction.SPC_TARGET_TAG, entity);
        host.onClose(
            () -> entity.setData(
                node.withActionData(data.toImmutable()).mergeInto(entity.getData()),
                player,
                false
            )
        );
        return true;
    };

    public static void editBlock(
        Instance instance,
        Point blockPosition,
        Block block,
        Consumer<ActionTriggerData> func
    ) {
        var node = block.getTag(TriggerNodeData.TAG);
        var data = node.actionData();
        func.accept(data);
        instance.setBlock(blockPosition, withNode(block, node.withActionData(data)));
    }

    private static void handleBlockInteract(Map2PlayerBlockInteractEvent event) {
        if (!(event.block().handler() instanceof PressurePlateBlock)) return;
        var node = event.block().getTag(TriggerNodeData.TAG);
        var type = actionType(node);
        if (type == null) return;

        event.setCancelled(true); // Always handle plates with an action list

        var player = event.player();
        var world = event.world();
        if (world.itemRegistry().isOnCooldown(player)) return;

        var data = node.actionData().toMutable();
        var host = Panel.open(player, new ActionEditorView(data, type));
        host.setTag(ActionEditorView.ACTION_LOCATION, event.blockPosition());
        host.setTag(CoordinateAction.SPC_TARGET_TAG, event.blockPosition());
        host.onClose(
            () -> world.instance().setBlock(
                event.blockPosition(),
                withNode(event.block(), node.withActionData(data.toImmutable()))
            )
        );
    }

    /// The action list the legacy kind of this trigger had; finish and reset triggers have none.
    private static @Nullable Action.Type actionType(TriggerNodeData node) {
        if (node.has(FinishPlayerInput.KEY) || node.has(ResetPlayerInput.KEY)) return null;
        return node.has(CheckpointPlayerInput.KEY) ? Action.Type.CHECKPOINT : Action.Type.STATUS;
    }

    /// Keeps the plate's other block entity data, such as its `legacy_id`.
    private static Block withNode(Block block, TriggerNodeData node) {
        return block.withNbt(
            node.mergeInto(Objects.requireNonNullElse(block.nbt(), CompoundBinaryTag.empty()))
        );
    }

    private static BlockItemHandler plateItem(String name, Block block, TriggerNodeData preset) {
        return new BlockItemHandler(
            "mapmaker:" + name,
            block.withHandler(PressurePlateBlock.INSTANCE).withTag(TriggerNodeData.TAG, preset),
            name
        );
    }

}
