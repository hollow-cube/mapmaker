package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class NpcItemModel extends BaseNpcEntity {
    private BiConsumer<Consumer<Player>, Player> addViewerHook;

    private boolean isStatic = false;

    public NpcItemModel() {
        this(UUID.randomUUID());
    }

    public NpcItemModel(@NotNull UUID uuid) {
        super(EntityType.ITEM_DISPLAY, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    /**
     * If true the entity will have its client view range increased significantly and never have its viewers removed.
     */
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        getEntityMeta().setViewRange(isStatic ? 10 : 1);
    }

    public void setModel(@NotNull Material material, int customModelData) {
        var meta = getEntityMeta();
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
        meta.setItemStack(ItemStack.builder(material).set(ItemComponent.CUSTOM_MODEL_DATA, customModelData).build());
    }

    @Override
    public @NotNull ItemDisplayMeta getEntityMeta() {
        return (ItemDisplayMeta) super.getEntityMeta();
    }

    public void setAddViewerHook(BiConsumer<Consumer<Player>, Player> addViewerHook) {
        this.addViewerHook = addViewerHook;
    }

    @Override
    public void updateNewViewer(@NotNull Player player) {
        if (addViewerHook != null) {
            addViewerHook.accept(super::updateNewViewer, player);
        } else {
            super.updateNewViewer(player);
        }
    }

    @Override
    public void updateOldViewer(@NotNull Player player) {
        if (isStatic) return;

        super.updateOldViewer(player);
    }
}
