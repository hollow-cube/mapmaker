package net.hollowcube.mapmaker.editor.parkour;

import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityEditor;
import net.hollowcube.mapmaker.map.event.Map2PlayerBlockInteractEvent;
import net.hollowcube.mapmaker.map.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorView;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.base.CoordinateAction;
import net.hollowcube.mapmaker.runtime.parkour.block.CheckpointPlateBlock;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.TagHandler;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class CheckpointEditor {

    public static final ItemHandler PLATE_ITEM = new BlockItemHandler(() -> CheckpointPlateBlock.INSTANCE,
            Block.HEAVY_WEIGHTED_PRESSURE_PLATE, "checkpoint_plate", CheckpointEditor::updateItemStack);

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(Map2PlayerBlockInteractEvent.class, CheckpointEditor::handleBlockInteract);

    public static final ObjectEntityEditor MARKER_EDITOR = (player, entity) -> {
        var checkpointData = Objects.requireNonNullElseGet(entity.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG), ActionTriggerData::new);
        var actionLocation = entity.getPosition().withY(y -> y + Objects.requireNonNullElse(entity.getMin(), Pos.ZERO).y());
        var host = Panel.open(player, new ActionEditorView(checkpointData.actions(), Action.Type.CHECKPOINT, "Checkpoint"));
        host.setTag(ActionEditorView.ACTION_LOCATION, actionLocation);
        host.setTag(CoordinateAction.SPC_TARGET_TAG, entity);
        host.onClose(() -> {
            entity.setTag(CheckpointPlateBlock.ENTITY_DATA_TAG, checkpointData);
            entity.handleDataChange(player);
        });
        return true;
    };

    public static void editBlock(Instance instance, Point blockPosition, Block block, Consumer<ActionTriggerData> func) {
        var data = block.getTag(CheckpointPlateBlock.DATA_TAG);
        func.accept(data);

        var newTag = TagHandler.newHandler();
        newTag.setTag(CheckpointPlateBlock.DATA_TAG, data);
        instance.setBlock(blockPosition, block.withNbt(newTag.asCompound()));
    }

    private static void handleBlockInteract(Map2PlayerBlockInteractEvent event) {
        if (!(event.block().handler() instanceof CheckpointPlateBlock))
            return;

        event.setCancelled(true); // Always handle checkpoint plates

        var player = event.player();
        var world = event.world();
        if (world.itemRegistry().isOnCooldown(player))
            return;

        var data = Objects.requireNonNullElseGet(event.block().getTag(CheckpointPlateBlock.DATA_TAG), ActionTriggerData::new);
        var host = Panel.open(player, new ActionEditorView(data.actions(), Action.Type.CHECKPOINT, "Checkpoint"));
        host.setTag(ActionEditorView.ACTION_LOCATION, event.blockPosition());
        host.setTag(CoordinateAction.SPC_TARGET_TAG, event.blockPosition());
        host.onClose(() -> {
            var newNbt = DFU.encodeNbt(ActionTriggerData.CODEC, data);
            world.instance().setBlock(event.blockPosition(), event.block().withNbt(newNbt));
        });
    }

    private static void updateItemStack(ItemStack.Builder builder, TagHandler tag) {
        var args = new ArrayList<Component>();
        var isEmpty = true;

        //todo reimplement

//        int resetHeight = tag.getTag(CheckpointSetting.RESET_HEIGHT);
//        args.add(CheckpointSetting.RESET_HEIGHT_TEXT_FUNCTION.apply(resetHeight));
//        if (resetHeight != -1) isEmpty = false;
//
//        // If the NBT has settings, set the lore to the "with data" variant, otherwise leave the default.
//        if (!isEmpty)
//            builder.lore(LanguageProviderV2.translateMulti("item.mapmaker.checkpoint_plate.with_data.lore", args)); //todo this translation key is remove, don't use it
//        builder.meta(m -> m.setTag(BlockItemHandler.BLOCK_DATA, tag.asCompound()));
    }

}
