package net.hollowcube.common.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minestom.server.item.Material;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class MaterialInfo {

    public static final MaterialMap<String> DYE_COLORS = new MaterialMap<>(adder -> {
        adder.accept(Material.WHITE_DYE, "white");
        adder.accept(Material.LIGHT_GRAY_DYE, "light_gray");
        adder.accept(Material.GRAY_DYE, "gray");
        adder.accept(Material.BLACK_DYE, "black");
        adder.accept(Material.BROWN_DYE, "brown");
        adder.accept(Material.RED_DYE, "red");
        adder.accept(Material.ORANGE_DYE, "orange");
        adder.accept(Material.YELLOW_DYE, "yellow");
        adder.accept(Material.LIME_DYE, "lime");
        adder.accept(Material.GREEN_DYE, "green");
        adder.accept(Material.CYAN_DYE, "cyan");
        adder.accept(Material.LIGHT_BLUE_DYE, "light_blue");
        adder.accept(Material.BLUE_DYE, "blue");
        adder.accept(Material.PURPLE_DYE, "purple");
        adder.accept(Material.MAGENTA_DYE, "magenta");
        adder.accept(Material.PINK_DYE, "pink");
    });

    private MaterialInfo() {
    }

    public record MaterialMap<T>(Int2ObjectMap<T> map) {

        public MaterialMap(Consumer<BiConsumer<Material, T>> factory) {
            this(Int2ObjectMaps.unmodifiable(OpUtils.build(new Int2ObjectArrayMap<>(), map ->
                factory.accept((material, value) -> map.put(material.id(), value))
            )));
        }

        public T get(Material material) {
            return map.get(material.id());
        }
    }
}
