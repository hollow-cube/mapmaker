package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NpcItemModel extends BaseNpcEntity {

    public NpcItemModel() {
        this(UUID.randomUUID());
    }

    public NpcItemModel(@NotNull UUID uuid) {
        super(EntityType.ITEM_DISPLAY, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    // 13
    // -6
    public void setModel(@NotNull Material material, int customModelData) {
        var meta = getEntityMeta();
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
        meta.setItemStack(ItemStack.builder(material).meta(b -> b.customModelData(customModelData)).build());
    }

    @Override
    public @NotNull ItemDisplayMeta getEntityMeta() {
        return (ItemDisplayMeta) super.getEntityMeta();
    }
}
