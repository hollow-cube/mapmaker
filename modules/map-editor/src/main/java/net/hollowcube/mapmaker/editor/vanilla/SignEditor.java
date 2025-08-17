package net.hollowcube.mapmaker.editor.vanilla;

import net.hollowcube.common.events.UpdateSignTextEvent;
import net.hollowcube.common.util.MaterialInfo;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.block.handler.SignBlockHandler;
import net.hollowcube.mapmaker.map.event.Map2PlayerBlockInteractEvent;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket;

import static net.hollowcube.mapmaker.map.block.handler.BlockHandlerHelpers.applyStoredBlockData;
import static net.hollowcube.mapmaker.map.block.handler.SignBlockHandler.BACK_TEXT;
import static net.hollowcube.mapmaker.map.block.handler.SignBlockHandler.FRONT_TEXT;
import static net.hollowcube.mapmaker.map.util.EventUtil.playerEventNode;

public class SignEditor {

    public static final EventNode<PlayerInstanceEvent> EVENT_NODE = playerEventNode()
            .addListener(PlayerBlockPlaceEvent.class, SignEditor::handlePlacement)
            .addListener(Map2PlayerBlockInteractEvent.class, SignEditor::handleInteraction)
            .addListener(UpdateSignTextEvent.class, SignEditor::handleTextUpdate);

    private static void handlePlacement(PlayerBlockPlaceEvent event) {
        // If the sign had NBT data attached, apply it to the block
        if (applyStoredBlockData(event)) return;


        // Otherwise, open the sign editor
        event.getPlayer().sendPacket(new OpenSignEditorPacket(event.getBlockPosition(), true));
    }

    private static void handleInteraction(Map2PlayerBlockInteractEvent event) {
        if (!(event.block().handler() instanceof SignBlockHandler))
            return;
        event.setCancelled(true);

        var player = event.player();
        var itemStack = player.getItemInHand(event.hand());

        var world = event.world();
        var blockPosition = event.blockPosition();
        var block = event.block();
        var instance = world.instance();
        var isFront = isFacingFront(block, blockPosition, player);

        if (world.itemRegistry().isOnCooldown(player)) return;

        var data = block.getTag(isFront ? FRONT_TEXT : BACK_TEXT);

        if (itemStack.material().equals(Material.GLOW_INK_SAC)) {
            data = data.withGlows(!data.hasGlowingText());
            instance.setBlock(blockPosition, block.withTag(isFront ? FRONT_TEXT : BACK_TEXT, data));
            return;
        }

        var dyeColor = MaterialInfo.DYE_COLORS.get(itemStack.material());
        if (dyeColor != null) {
            data = data.withColor(dyeColor);
            instance.setBlock(blockPosition, block.withTag(isFront ? FRONT_TEXT : BACK_TEXT, data));
            return;
        }

        // Skip sign editing if player is sneaking
        if (!player.isSneaking()) {
            // Set the data to the actual data and then open the editor and then set it back
            var realBlockData = CompoundBinaryTag.builder();
            realBlockData.put(isFront ? "front_text" : "back_text", data.toNbt());

            player.sendPacket(new BundlePacket());
            player.sendPacket(new BlockEntityDataPacket(blockPosition, 7, realBlockData.build()));
            player.sendPacket(new OpenSignEditorPacket(blockPosition, isFront));
            player.sendPacket(new BundlePacket());
            return;
        }

        // Didn't end up doing anything, so can let the next interaction happen
        event.setCancelled(false);
    }

    private static void handleTextUpdate(UpdateSignTextEvent event) {
        var instance = event.getInstance();
        var pos = event.position();

        var block = instance.getBlock(pos);
        if (!(block.handler() instanceof SignBlockHandler)) return;

        var map = EditorMapWorld.forPlayer(event.getPlayer());
        if (map == null) return;

        var tag = event.isFrontText() ? FRONT_TEXT : BACK_TEXT;
        var data = block.getTag(tag);

        var lines = event.lines().stream().map(Component::text).toArray(Component[]::new);

        instance.setBlock(pos, block.withTag(tag, data.withLines(lines)));
    }

    private static boolean isFacingFront(Block block, Point blockPosition, Player player) {
        var relative = player.getPosition().sub(blockPosition.add(getBlockCenter(block)));
        return Math.abs(wrapDegrees((Math.atan2(relative.z(), relative.x()) * 57.2957763671875) - 90f - getBlockAngle(block))) <= 90f;
    }

    private static Point getBlockCenter(Block block) {
        if (BlockTags.STANDING_SIGNS.contains(block.key())) {
            return new Vec(0.5);
        } else if (BlockTags.WALL_SIGNS.contains(block.key()) || BlockTags.ALL_HANGING_SIGNS.contains(block.key())) {
            var shape = block.registry().collisionShape();
            return shape.relativeStart().add(shape.relativeEnd()).div(2); // TODO THIS IS NOT PERFECT
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    private static double getBlockAngle(Block block) {
        if (BlockTags.STANDING_SIGNS.contains(block.key()) || BlockTags.CEILING_HANGING_SIGNS.contains(block.key())) {
            return Integer.parseInt(block.getProperty("rotation")) * 22.5;
        } else if (BlockTags.WALL_SIGNS.contains(block.key()) || BlockTags.WALL_HANGING_SIGNS.contains(block.key())) {
            // TODO: move this block face to direction to some common util
            return switch (block.getProperty("facing")) {
                case "south" -> 0;
                case "west" -> 90;
                case "north" -> 180;
                case "east" -> 270;
                case "down", "up" -> 270; // This case is from vanilla
                default -> throw new IllegalStateException("unreachable");
            };
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    public static double wrapDegrees(double angle) {
        double wrapped = angle % 360.0;
        if (wrapped >= 180.0) wrapped -= 360.0;
        if (wrapped < -180.0) wrapped += 360.0;
        return wrapped;
    }
}
