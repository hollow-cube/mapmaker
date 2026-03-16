package net.hollowcube.mapmaker.punishments.types;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

@RuntimeGson
public record PunishmentLadder(
    String id,
    String name,
    PunishmentType type,
    List<Entry> entries,
    List<Reason> reasons
) {

    @RuntimeGson
    public record Entry(long duration) {
    }

    @RuntimeGson
    public record Reason(String id, List<String> aliases) {
    }
}
