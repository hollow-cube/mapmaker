package net.hollowcube.mapmaker.map.item;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class ItemTags {

    public static Collection<Key> SWORDS = builtin("minecraft:swords");
    public static Collection<Key> SHOVELS = builtin("minecraft:shovels");
    public static Collection<Key> HOES = builtin("minecraft:hoes");
    public static Collection<Key> AXES = builtin("minecraft:axes");
    public static Collection<Key> LEAVES = builtin("minecraft:leaves");
    public static Collection<Key> SAPLINGS = builtin("minecraft:saplings");
    public static Collection<Key> BOOKSHELF_BOOKS = builtin("minecraft:bookshelf_books");
    public static Collection<Key> SPAWN_EGGS = create("spawn_egg");
    public static Collection<Key> SHERDS = create("_sherd");


    private static @NotNull Collection<Key> builtin(@NotNull String name) {
        var tag = MinecraftServer.getTagManager().getTag(Tag.BasicType.ITEMS, name);
        Check.notNull(tag, "Item tag " + name + " is not registered");
        return tag.getValues();
    }

    private static @NotNull Collection<Key> create(@NotNull Material... material) {
        var set = new HashSet<Key>();
        for (var m : material) {
            set.add(m.key());
        }
        return Set.copyOf(set);
    }

    // Used to grab all materials with a certain suffix
    private static @NotNull Collection<Key> create(@NotNull String suffix) {
        var set = new HashSet<Key>();
        for (var m : Material.values()) {
            if (m.name().endsWith(suffix)) {
                set.add(m.key());
            }
        }
        return Set.copyOf(set);
    }

}
