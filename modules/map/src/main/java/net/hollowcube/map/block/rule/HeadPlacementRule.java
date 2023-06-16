package net.hollowcube.map.block.rule;

import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.instance.block.rule.BlockPlacementRule;
import net.minestom.server.item.ItemMeta;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTType;

public class HeadPlacementRule extends BlockPlacementRule {

    public HeadPlacementRule(@NotNull Block block) {
        super(block);
    }

    @Override
    public Block blockPlace(@NotNull PlacementState placementState) {
        var usedItemMeta = placementState.usedItemMeta();

        var blockFace = placementState.blockFace();
        if (blockFace == BlockFace.TOP || blockFace == BlockFace.BOTTOM) {
            float yaw = placementState.playerPosition().yaw() + 360;
            int rotation = (int) (Math.round(yaw / 22.5d) % 16);

            return withSkin(usedItemMeta, block)
                    .withProperty("rotation", String.valueOf(rotation));
        }

        return withSkin(usedItemMeta, toWallBlock(block))
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

        return Block.fromNamespaceId(rawName + "_wall" + rawType);
    }

    /**
     * Include the head skin tags if present.
     *
     * @param meta  the original head meta
     * @param block the block
     * @return the block with the skin tags
     */
    private Block withSkin(ItemMeta meta, Block block) {
        var skullOwner = meta.getTag(PlayerHeadMeta.SKULL_OWNER);
        if (skullOwner == null) return block;

        var skin = meta.getTag(PlayerHeadMeta.SKIN);
        if (skin == null) return block;

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
        return block.withTag(Tag.NBT("SkullOwner"), NBT.Compound(tag -> {
            Tag.UUID("Id").write(tag, skullOwner);
            tag.set("Properties", NBT.Compound(propTag ->
                    propTag.set("textures", NBT.List(NBTType.TAG_Compound,
                            NBT.Compound(txTag -> txTag.setString("Value", textures))))));
        }));
    }

}
