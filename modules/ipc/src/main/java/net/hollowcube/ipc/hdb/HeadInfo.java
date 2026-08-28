package net.hollowcube.ipc.hdb;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

/// One head as the head database stores it.
///
/// `texture` is the bare texture hash, not a profile: turning it into something a player can hold
/// belongs to whoever is holding it, and keeping this record free of Minestom is what lets the
/// api-server serve it.
@RuntimeGson
public record HeadInfo(
    String id,
    String name,
    String category,
    String texture,
    List<String> tags
) {
}
