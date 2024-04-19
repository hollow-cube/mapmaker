package net.hollowcube.mapmaker.map.block.placement;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.UniqueIdUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class HeadPlacementRule extends BaseBlockPlacementRule {
    private static final Tag<BinaryTag> SKULL_OWNER = Tag.NBT("SkullOwner");

    public HeadPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public Block blockPlace(@NotNull PlacementState placementState) {
        var usedItemStack = placementState.usedItemStack();

        var blockFace = Objects.requireNonNullElse(placementState.blockFace(), BlockFace.TOP);
        if (blockFace == BlockFace.TOP || blockFace == BlockFace.BOTTOM) {
            var playerPosition = Objects.requireNonNullElse(placementState.playerPosition(), Pos.ZERO);
            int rotation = (int) (Math.round((playerPosition.yaw() + 360) / 22.5d) % 16);

            return withSkin(usedItemStack, block)
                    .withProperty("rotation", String.valueOf(rotation));
        }

        return withSkin(usedItemStack, toWallBlock(block))
                .withProperty("facing", blockFace.name().toLowerCase());
    }

    /**
     * Convert the given head/skull block into its wall variant.
     *
     * @param block the block to convert
     * @return the wall variant of the block
     */
    private Block toWallBlock(Block block) {
        // Is there a better way to do this?
        String name = block.namespace().value();

        // player_head -> player
        String rawName = name.substring(0, name.lastIndexOf("_"));
        // player_head -> _head
        String rawType = name.substring(rawName.length());

        return Block.fromNamespaceId(rawName + "_wall" + rawType)
                .withHandler(block.handler());
    }

    /**
     * Include the head skin tags if present.
     *
     * @param itemStack the original head item stack
     * @param block     the block
     * @return the block with the skin tags
     */
    private @NotNull Block withSkin(@Nullable ItemStack itemStack, @NotNull Block block) {
        if (itemStack == null) return block;

        var profile = itemStack.get(ItemComponent.PROFILE);
        if (profile == null) return block;
        var skin = profile.skin();
        String textures = skin.textures();
        if (textures == null) return block;

        /*
            SkullOwner (Compound)
                |_ Id (UUID)
                |_ Properties (Compound)
                    |_ textures (Compound List)
                        |_ Value (String)
            See https://minecraft.fandom.com/wiki/Head#Block_data
         */
        var builder = CompoundBinaryTag.builder();
        if (profile.uuid() != null)
            builder.put("Id", UniqueIdUtils.toNbt(profile.uuid()));
        builder.put("Properties", CompoundBinaryTag.builder()
                .put("textures", ListBinaryTag.builder(BinaryTagTypes.COMPOUND)
                        .add(CompoundBinaryTag.builder()
                                .putString("Value", textures)
                                .build())
                        .build())
                .build());

        return block.withTag(SKULL_OWNER, builder.build());
    }

}
