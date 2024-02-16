package net.hollowcube.map2.block.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.block.BlockTags;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static net.hollowcube.map2.block.handler.BlockHandlerHelpers.applyStoredBlockData;

public class SignBlockHandler implements BlockHandler {
    private record SignData(
            boolean hasGlowingText,
            String color,
            Component[] lines // Always 4 long
    ) {
        private static final TagSerializer<SignData> SERIALIZER = new TagSerializer<>() {
            private static final List<Component> DEFAULT_MESSAGES = List.of(Component.empty(), Component.empty(), Component.empty(), Component.empty());
            private static final Tag<Boolean> HAS_GLOWING_TEXT = Tag.Boolean("has_glowing_text").defaultValue(false);
            private static final Tag<String> COLOR = Tag.String("color").defaultValue("black");
            private static final Tag<List<Component>> LINES = Tag.Component("messages").list().defaultValue(DEFAULT_MESSAGES);

            @Override
            public @NotNull SignData read(@NotNull TagReadable reader) {
                return new SignData(
                        reader.getTag(HAS_GLOWING_TEXT),
                        reader.getTag(COLOR),
                        reader.getTag(LINES).toArray(Component[]::new)
                );
            }

            @Override
            public void write(@NotNull TagWritable writer, @NotNull SignData value) {
                writer.setTag(HAS_GLOWING_TEXT, value.hasGlowingText);
                writer.setTag(COLOR, value.color);
                writer.setTag(LINES, List.of(value.lines));
            }
        };
    }

    private static final Tag<Boolean> IS_WAXED = Tag.Boolean("is_waxed").defaultValue(false);
    private static final Tag<SignData> FRONT_TEXT = Tag.Structure("front_text", SignData.SERIALIZER)
            .defaultValue(new SignData(false, "black", new Component[0]));
    private static final Tag<SignData> BACK_TEXT = Tag.Structure("back_text", SignData.SERIALIZER)
            .defaultValue(new SignData(false, "black", new Component[0]));

    private static final Int2ObjectMap<String> DYE_MAP = new Int2ObjectArrayMap<>();

    static {
        var packetListenerManager = MinecraftServer.getPacketListenerManager();
        packetListenerManager.setListener(ClientUpdateSignPacket.class, SignBlockHandler::handleUpdateSignPacket);

        DYE_MAP.put(Material.WHITE_DYE.id(), "white");
        DYE_MAP.put(Material.LIGHT_GRAY_DYE.id(), "light_gray");
        DYE_MAP.put(Material.GRAY_DYE.id(), "gray");
        DYE_MAP.put(Material.BLACK_DYE.id(), "black");
        DYE_MAP.put(Material.BROWN_DYE.id(), "brown");
        DYE_MAP.put(Material.RED_DYE.id(), "red");
        DYE_MAP.put(Material.ORANGE_DYE.id(), "orange");
        DYE_MAP.put(Material.YELLOW_DYE.id(), "yellow");
        DYE_MAP.put(Material.LIME_DYE.id(), "lime");
        DYE_MAP.put(Material.GREEN_DYE.id(), "green");
        DYE_MAP.put(Material.CYAN_DYE.id(), "cyan");
        DYE_MAP.put(Material.LIGHT_BLUE_DYE.id(), "light_blue");
        DYE_MAP.put(Material.BLUE_DYE.id(), "blue");
        DYE_MAP.put(Material.PURPLE_DYE.id(), "purple");
        DYE_MAP.put(Material.MAGENTA_DYE.id(), "magenta");
        DYE_MAP.put(Material.PINK_DYE.id(), "pink");
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
        var packet = new OpenSignEditorPacket(blockPosition, true);
        p.getPlayer().sendPacket(packet);
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
        if (world == null || !world.canEdit(player))
            return false;

        if (itemStack.material().equals(Material.GLOW_INK_SAC)) {
            var signData = block.getTag(isFront ? FRONT_TEXT : BACK_TEXT);
            signData = new SignData(!signData.hasGlowingText, signData.color, signData.lines);
            block = block.withTag(isFront ? FRONT_TEXT : BACK_TEXT, signData);
            instance.setBlock(blockPosition, block);
            return false;
        }

        var dyeColor = DYE_MAP.get(itemStack.material().id());
        if (dyeColor != null) {
            var signData = block.getTag(isFront ? FRONT_TEXT : BACK_TEXT);
            signData = new SignData(signData.hasGlowingText, dyeColor, signData.lines);
            block = block.withTag(isFront ? FRONT_TEXT : BACK_TEXT, signData);
            instance.setBlock(blockPosition, block);
            System.out.println(signData + " " + Arrays.toString(signData.lines));
            return false;
        }

        // Handle editing the sign
        var packet = new OpenSignEditorPacket(blockPosition, isFront);
        var playerMap = MapWorld.forPlayerOptional(player);
        if (!(playerMap == null) && !playerMap.map().isPublished()) {
            player.sendPacket(packet);
        }
        return true;
    }

    @Override
    public @NotNull Collection<Tag<?>> getBlockEntityTags() {
        return List.of(IS_WAXED, FRONT_TEXT, BACK_TEXT);
    }

    public static void handleUpdateSignPacket(@NotNull ClientUpdateSignPacket packet, @NotNull Player player) {
        var instance = player.getInstance();
        if (instance == null) return;

        var blockPosition = packet.blockPosition();
        var block = instance.getBlock(blockPosition);
        if (block.handler() != net.hollowcube.map2.block.handler.BlockHandlers.SIGN && block.handler() != net.hollowcube.map2.block.handler.BlockHandlers.HANGING_SIGN)
            return;

        var tag = packet.isFrontText() ? FRONT_TEXT : BACK_TEXT;
        var signData = block.getTag(tag);
        var lines = packet.lines().stream().map(Component::text).toArray(Component[]::new);
        block = block.withTag(tag, new SignData(signData.hasGlowingText, signData.color, lines));

        instance.setBlock(blockPosition, block);
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
