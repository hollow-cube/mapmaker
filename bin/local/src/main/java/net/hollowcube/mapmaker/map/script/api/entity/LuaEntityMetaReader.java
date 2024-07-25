package net.hollowcube.mapmaker.map.script.api.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.script.api.item.ItemStackTypeImpl;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.EntityMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import org.jetbrains.annotations.NotNull;

public final class LuaEntityMetaReader {

    public static void applyMeta(@NotNull Entity entity, @NotNull LuaState state, int tableIndex) {
        state.pushNil();
        while (state.next(tableIndex)) {
            // Key is at index -2, value is at index -1
            String key = state.toString(-2);
            applyAnyMeta(entity.getEntityMeta(), key, state, -1);

            // Remove the value, keep the key for the next iteration
            state.pop(1);
        }
    }

    private static void applyAnyMeta(@NotNull EntityMeta meta, @NotNull String key, @NotNull LuaState state, int valueIndex) {
        switch (meta) {
            case ItemDisplayMeta itemDisplay -> applyItemDisplayMeta(itemDisplay, key, state, valueIndex);
            default -> applyEntityMeta(meta, key, state, valueIndex);
        }
    }

    private static void applyItemDisplayMeta(@NotNull ItemDisplayMeta meta, @NotNull String key, @NotNull LuaState state, int valueIndex) {
        switch (key) {
            case "item" -> meta.setItemStack(ItemStackTypeImpl.checkLuaArg(state, valueIndex));
            default -> applyEntityMeta(meta, key, state, valueIndex);
        }
    }

    private static void applyEntityMeta(@NotNull EntityMeta meta, @NotNull String key, @NotNull LuaState state, int valueIndex) {
        switch (key) {
            default -> state.error("Unknown entity meta key: " + key);
        }
    }

}
