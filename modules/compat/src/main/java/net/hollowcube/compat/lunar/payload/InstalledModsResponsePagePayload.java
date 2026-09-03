package net.hollowcube.compat.lunar.payload;

import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;

import java.util.List;

public record InstalledModsResponsePagePayload(
    String id,
    int page,
    int totalPages,
    List<InstalledModsResponsePayload.ModGroup> groups
) implements LunarPayload.Paginated<InstalledModsResponsePagePayload> {

    public static final String ID = "lunarclient.apollo.modsetting.v1.InstalledModsResponse";
    public static final StructCodec<InstalledModsResponsePagePayload> CODEC = StructCodec.struct(
        "request_id", Codec.STRING, InstalledModsResponsePagePayload::id,
        "page", Codec.INT, InstalledModsResponsePagePayload::page,
        "total_pages", Codec.INT, InstalledModsResponsePagePayload::totalPages,
        "mod_groups", InstalledModsResponsePayload.ModGroup.CODEC.list(), InstalledModsResponsePagePayload::groups,
        InstalledModsResponsePagePayload::new
    );
    public static final LunarPayloadType<InstalledModsResponsePagePayload> TYPE = new LunarPayloadType<>(ID, CODEC);

    @Override
    public LunarPayload group(List<InstalledModsResponsePagePayload> pages) {
        return InstalledModsResponsePayload.fromPages(pages);
    }
}
