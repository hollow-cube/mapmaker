package net.hollowcube.mapmaker.map.block.handler;

import net.hollowcube.common.events.UpdateSignTextEvent;
import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.common.util.MaterialInfo;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.BlockTags;
import net.hollowcube.mapmaker.map.block.handler.sign.SignData;
import net.hollowcube.mapmaker.map.util.InteractTarget;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.BundlePacket;
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static net.hollowcube.mapmaker.map.block.handler.BlockHandlerHelpers.applyStoredBlockData;

public class SignBlockHandler implements BlockHandler, InteractTarget {

    public static final Tag<Boolean> IS_WAXED = Tag.Boolean("is_waxed").defaultValue(false);
    public static final Tag<SignData> FRONT_TEXT = Tag.Structure("front_text", SignData.SERIALIZER).defaultValue(SignData.empty());
    public static final Tag<SignData> BACK_TEXT = Tag.Structure("back_text", SignData.SERIALIZER).defaultValue(SignData.empty());

    static {
        MinecraftServer.getGlobalEventHandler()
                .addListener(UpdateSignTextEvent.class, SignBlockHandler::handleUpdateSignPacket);
    }

    private final NamespaceID id;

    SignBlockHandler(@NotNull String id) {
        this.id = NamespaceID.from(id);
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return id;
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        if (!(placement instanceof PlayerPlacement p)) return;

        // If the sign had NBT data attached, apply it to the block
        if (applyStoredBlockData(p)) return;

        // Otherwise, open the sign editor
        var blockPosition = placement.getBlockPosition();
        p.getPlayer().sendPacket(new OpenSignEditorPacket(blockPosition, true));
    }

    @Override
    public boolean onInteract(@NotNull Interaction interaction) {
        var player = interaction.getPlayer();
        var itemStack = player.getItemInHand(interaction.getHand());

        var blockPosition = interaction.getBlockPosition();
        var block = interaction.getBlock();
        var instance = interaction.getInstance();
        var isFront = isFacingFront(block, blockPosition, player);

        MapWorld world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return false;
        if (world.itemRegistry().isOnCooldown(player)) return true;

        var data = block.getTag(isFront ? FRONT_TEXT : BACK_TEXT);

        if (itemStack.material().equals(Material.GLOW_INK_SAC)) {
            data = data.withGlows(!data.hasGlowingText());
            instance.setBlock(blockPosition, block.withTag(isFront ? FRONT_TEXT : BACK_TEXT, data));
            return false;
        }

        var dyeColor = MaterialInfo.DYE_COLORS.get(itemStack.material());
        if (dyeColor != null) {
            data = data.withColor(dyeColor);
            instance.setBlock(blockPosition, block.withTag(isFront ? FRONT_TEXT : BACK_TEXT, data));
            return false;
        }

        // Skip sign editing if player is sneaking
        if (!player.isSneaking()) {
            // Handle editing the sign
            var map = MapWorld.forPlayerOptional(player);
            if (map != null && !map.map().isPublished()) {
                // Set the data to the actual data and then open the editor and then set it back
                var realBlockData = CompoundBinaryTag.builder();
                realBlockData.put(isFront ? "front_text" : "back_text", data.toNbt());

                player.sendPacket(new BundlePacket());
                player.sendPacket(new BlockEntityDataPacket(blockPosition, 7, realBlockData.build()));
                player.sendPacket(new OpenSignEditorPacket(blockPosition, isFront));
                player.sendPacket(new BundlePacket());

                return false;
            }
        }
        return true;
    }

    @Override
    public byte getBlockEntityAction() {
        return 3;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(
                IS_WAXED,
                ExtraTags.MappedView(FRONT_TEXT, SignData::withFormatting),
                ExtraTags.MappedView(BACK_TEXT, SignData::withFormatting)
        );
    }

    public static void handleUpdateSignPacket(@NotNull UpdateSignTextEvent event) {
        var instance = event.getInstance();
        var pos = event.position();

        var block = instance.getBlock(pos);
        if (!(block.handler() instanceof SignBlockHandler)) return;

        var map = MapWorld.forPlayerOptional(event.getPlayer());
        if (map == null || !map.canEdit(event.getPlayer())) return;

        var tag = event.isFrontText() ? FRONT_TEXT : BACK_TEXT;
        var data = block.getTag(tag);

        var lines = event.lines().stream().map(Component::text).toArray(Component[]::new);

        instance.setBlock(pos, block.withTag(tag, data.withLines(lines)));
    }

    private boolean isFacingFront(@NotNull Block block, @NotNull Point blockPosition, @NotNull Player player) {
        var relative = player.getPosition().sub(blockPosition.add(getBlockCenter(block)));
        return Math.abs(wrapDegrees((Math.atan2(relative.z(), relative.x()) * 57.2957763671875) - 90f - getBlockAngle(block))) <= 90f;
    }

    private Point getBlockCenter(@NotNull Block block) {
        if (BlockTags.STANDING_SIGNS.contains(block.namespace())) {
            return new Vec(0.5);
        } else if (BlockTags.WALL_SIGNS.contains(block.namespace()) || BlockTags.ALL_HANGING_SIGNS.contains(block.namespace())) {
            var shape = block.registry().collisionShape();
            return shape.relativeStart().add(shape.relativeEnd()).div(2); // TODO THIS IS NOT PERFECT
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

    private double getBlockAngle(@NotNull Block block) {
        if (BlockTags.STANDING_SIGNS.contains(block.namespace()) || BlockTags.CEILING_HANGING_SIGNS.contains(block.namespace())) {
            return Integer.parseInt(block.getProperty("rotation")) * 22.5;
        } else if (BlockTags.WALL_SIGNS.contains(block.namespace()) || BlockTags.WALL_HANGING_SIGNS.contains(block.namespace())) {
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
