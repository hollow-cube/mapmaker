package net.hollowcube.map2.item.handler;

import net.minestom.server.MinecraftServer;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class ItemTags {

    public static Collection<NamespaceID> SWORDS = builtin("minecraft:swords");
    public static Collection<NamespaceID> SHOVELS = builtin("minecraft:shovels");
    public static Collection<NamespaceID> HOES = builtin("minecraft:hoes");
    public static Collection<NamespaceID> AXES = builtin("minecraft:axes");
    public static Collection<NamespaceID> LEAVES = builtin("minecraft:leaves");
    public static Collection<NamespaceID> SAPLINGS = builtin("minecraft:saplings");


    private static @NotNull Collection<NamespaceID> builtin(@NotNull String name) {
        var tag = MinecraftServer.getTagManager().getTag(Tag.BasicType.ITEMS, name);
        Check.notNull(tag, "Item tag " + name + " is not registered");
        return tag.getValues();
    }

    private static @NotNull Collection<NamespaceID> create(@NotNull Material... material) {
        var set = new HashSet<NamespaceID>();
        for (var m : material) {
            set.add(m.namespace());
        }
        return Set.copyOf(set);
    }

}
