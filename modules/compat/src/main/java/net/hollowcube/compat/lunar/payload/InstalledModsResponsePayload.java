package net.hollowcube.compat.lunar.payload;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record InstalledModsResponsePayload(List<ModGroup> groups) implements LunarPayload {

    public static InstalledModsResponsePayload fromPages(List<InstalledModsResponsePagePayload> pages) {
        Map<String, List<Mod>> groups = new HashMap<>();
        for (var page : pages) {
            for (var group : page.groups()) {
                groups.computeIfAbsent(group.type(), _ -> new ArrayList<>()).addAll(group.mods());
            }
        }

        return new InstalledModsResponsePayload(
            groups.entrySet()
                .stream()
                .map(e -> new ModGroup(e.getKey(), e.getValue()))
                .toList()
        );
    }

    public record ModGroup(String type, List<Mod> mods) {

        public static final StructCodec<ModGroup> CODEC = StructCodec.struct(
            "type", Codec.STRING, ModGroup::type,
            "mods", Mod.CODEC.list(), ModGroup::mods,
            ModGroup::new
        );
    }

    public record Mod(String id, String version) {

        public static final StructCodec<Mod> CODEC = StructCodec.struct(
            "id", Codec.STRING, Mod::id,
            "version", Codec.STRING, Mod::version,
            Mod::new
        );
    }
}
