package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.common.util.JsonUtil;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

public class StatusPlateBlock implements BlockHandler, PressurePlateBlock, DebugCommand.BlockDebug {
    private static final Key KEY = Key.key("mapmaker:status_plate");

    public static final StatusPlateBlock INSTANCE = new StatusPlateBlock();

    public static final Tag<ActionTriggerData> DATA_TAG = DFU.View(ActionTriggerData.CODEC);
    public static final Tag<ActionTriggerData> ENTITY_DATA_TAG = DFU.Tag(ActionTriggerData.CODEC, "status").path("data");

    public static final TagCooldown APPLY_COOLDOWN = new TagCooldown("mapmaker:status_plate_cooldown", 250);

    private StatusPlateBlock() {
    }

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public void onEnter(Collision collision) {
        final var player = collision.player();
        if (!StatusPlateBlock.APPLY_COOLDOWN.test(player)) return;

        final var data = collision.block().getTag(DATA_TAG);
        TempEffectApplicator.applyStatus(data, player, createId(collision.blockPosition()));
    }

    @Override
    public void sendDebugInfo(Player player, Block block) {
        var data = block.getTag(DATA_TAG);
        var info = JsonUtil.toPrettyJson(ActionTriggerData.CODEC, data);
        if (info == null) {
            player.sendMessage("No status data found for this block.");
        } else {
            player.sendMessage("Data: " + info);
        }
    }

    private static String createId(Point blockPosition) {
        // This is a legacy formatting, but was stored in save states so its relevant to preserve.
        return String.format("mapmaker:status_plate/%d_%d_%d",
                blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ());
    }

}
