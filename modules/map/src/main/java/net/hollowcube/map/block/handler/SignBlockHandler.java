package net.hollowcube.map.block.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.map.block.BlockTags;
import net.hollowcube.map.world.MapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
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

public class SignBlockHandler implements BlockHandler {
    private record SignData(
            boolean hasGlowingText,
            String color,
            Component[] lines // Always 4 long
    ) {
        private static final TagSerializer<SignData> SERIALIZER = new TagSerializer<>() {
            private static final List<Component> DEFAULT_MESSAGES = List.of(Component.text(""), Component.text(""), Component.text(""), Component.text(""));

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

    public static final NamespaceID ID = NamespaceID.from("minecraft:sign");
    public static final SignBlockHandler INSTANCE = new SignBlockHandler();

    private static final Int2ObjectMap<String> DYE_MAP = new Int2ObjectArrayMap<>();

    static {
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

    private SignBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        if (!(placement instanceof PlayerPlacement p)) return;

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

        boolean isFront;
        if (BlockTags.STANDING_SIGNS.contains(block.namespace())) {
            var playerPos = Vec.fromPoint(player.getPosition());
            var blockCenterPos = Vec.fromPoint(blockPosition).add(0.5f);
            var angle = Math.toDegrees(Math.atan2(blockCenterPos.z() - playerPos.z(), blockCenterPos.x() - playerPos.x()));

            var blockRotation = Integer.parseInt(block.getProperty("rotation"));
            var adjustedAngle = angle - (blockRotation * 22.5) + 360;

            isFront = adjustedAngle < 0;
        } else {
            System.out.println("NOT A SIGN " + block.name());
            return false;
        }

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
        if (block.handler() != SignBlockHandler.INSTANCE) return;

        var tag = packet.isFrontText() ? FRONT_TEXT : BACK_TEXT;
        var signData = block.getTag(tag);
        var lines = packet.lines().stream().map(Component::text).toArray(Component[]::new);
        block = block.withTag(tag, new SignData(signData.hasGlowingText, signData.color, lines));

        instance.setBlock(blockPosition, block);
    }
}
