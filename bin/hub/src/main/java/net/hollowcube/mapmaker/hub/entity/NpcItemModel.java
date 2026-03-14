package net.hollowcube.mapmaker.hub.entity;

import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public class NpcItemModel extends BaseNpcEntity {
    private @Nullable BiConsumer<Consumer<Player>, Player> addViewerHook;

    private boolean isStatic = false;

    public NpcItemModel() {
        this(UUID.randomUUID());
    }

    public NpcItemModel(UUID uuid) {
        super(EntityType.ITEM_DISPLAY, uuid);

        // Minestom doesnt handle entity sync correctly for display entities, it resets interpolation
        setSynchronizationTicks(Long.MAX_VALUE);

        hasPhysics = false;
        setNoGravity(true);
    }

    @Override
    public void tick(long time) {
        // Intentionally do nothing
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }

    /**
     * If true the entity will have its client view range increased significantly and never have its viewers removed.
     */
    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        getEntityMeta().setViewRange(isStatic ? 10 : 1);
    }

    public void setModel(BadSprite sprite) {
        setModel(Material.STICK, sprite);
    }

    public void setModel(Material material, BadSprite sprite) {
        var meta = getEntityMeta();
        meta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
        var model = Objects.requireNonNull(sprite.model(), "sprite must have a model");
        meta.setItemStack(ItemStack.builder(material).set(DataComponents.ITEM_MODEL, model).build());
    }

    @Override
    public ItemDisplayMeta getEntityMeta() {
        return (ItemDisplayMeta) super.getEntityMeta();
    }

    public void editEntityMeta(Consumer<ItemDisplayMeta> editor) {
        super.editEntityMeta(ItemDisplayMeta.class, editor);
    }

    public void setAddViewerHook(BiConsumer<Consumer<Player>, Player> addViewerHook) {
        this.addViewerHook = addViewerHook;
    }

    @Override
    public void updateNewViewer(Player player) {
        if (addViewerHook != null) {
            addViewerHook.accept(super::updateNewViewer, player);
        } else {
            super.updateNewViewer(player);
        }
    }

    @Override
    public void updateOldViewer(Player player) {
        if (isStatic) return;

        super.updateOldViewer(player);
    }
}
