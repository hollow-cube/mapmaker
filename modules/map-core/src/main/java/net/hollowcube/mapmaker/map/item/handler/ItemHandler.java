package net.hollowcube.mapmaker.map.item.handler;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.BlockFace;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.List;
import java.util.function.Consumer;

/**
 * Similar interface to {@link net.minestom.server.instance.block.BlockHandler}, but for items. Like block handlers,
 * implementations should be implemented as singletons, and registered to the appropriate {@link ItemRegistry} for
 * use within a {@link MapWorld}.
 * <p>
 * Items are identified by their {@link #id()}, which is hashed to set the custom model data of the item stack.
 */
public abstract class ItemHandler {

    public static final int RIGHT_CLICK_AIR = 1 << 1;
    public static final int RIGHT_CLICK_BLOCK = 1 << 2;
    public static final int RIGHT_CLICK_ENTITY = 1 << 3;
    public static final int LEFT_CLICK_AIR = 1 << 4;
    public static final int LEFT_CLICK_BLOCK = 1 << 5;
    public static final int LEFT_CLICK_ENTITY = 1 << 6;

    public static final int RIGHT_CLICK_ANY = RIGHT_CLICK_AIR | RIGHT_CLICK_BLOCK | RIGHT_CLICK_ENTITY;
    public static final int LEFT_CLICK_ANY = LEFT_CLICK_AIR | LEFT_CLICK_BLOCK | LEFT_CLICK_ENTITY;

    private final NamespaceID id;
    private final int flags;

    protected ItemHandler(@NotNull String id, int... flags) {
        this.id = NamespaceID.from(id);

        int flag = 0;
        for (int f : flags) flag |= f;
        this.flags = flag;
    }

    public @NotNull NamespaceID id() {
        return id;
    }

    public int customModelData() {
        return id.hashCode();
    }

    public @NotNull Material material() {
        return Material.STICK;
    }

    public @NotNull ItemStack buildItemStack(@Nullable NBTCompound nbt) {
        var builder = ItemStack.builder(material());
        var baseTranslationKey = String.format("item.%s.%s", id().namespace(), id().path());
        builder.displayName(Component.translatable(baseTranslationKey + ".name"));
        builder.lore(LanguageProviderV2.translateMulti(baseTranslationKey + ".lore", List.of()));
        updateItemStack(builder, nbt != null ? TagHandler.fromCompound(nbt) : TagHandler.newHandler());
        builder.meta(meta -> meta.customModelData(customModelData()));
        return builder.build();
    }

    /**
     * updateItemStack is responsible for updating the base item with any custom data based on NBT.
     * <p>
     * By default, the builder will have the name and lore set which can be overridden.
     * The custom model data is set after this function, and will overwrite any value set here.
     */
    protected void updateItemStack(@NotNull ItemStack.Builder builder, @NotNull TagHandler data) {
    }

    protected void leftClicked(@NotNull Click click) {
    }

    protected void rightClicked(@NotNull Click click) {
    }

    protected final boolean allows(int flag) {
        return (flags & flag) != 0;
    }

    public record Click(
            @NotNull ItemHandler handler,
            @NotNull Player player,
            @NotNull ItemStack itemStack,
            @NotNull Player.Hand hand,
            @UnknownNullability Point blockPosition,
            @UnknownNullability Point placePosition,
            @UnknownNullability BlockFace face,
            @UnknownNullability Entity entity
    ) {
        public @UnknownNullability Instance instance() {
            return player.getInstance();
        }

        public void updateItemStack(@NotNull Consumer<ItemStack.Builder> func) {
            var updatedItemStack = itemStack.with(builder -> {
                func.accept(builder);
                builder.meta(meta -> meta.customModelData(handler.customModelData()));
            });
            player.setItemInHand(hand, updatedItemStack);
        }
    }

}
