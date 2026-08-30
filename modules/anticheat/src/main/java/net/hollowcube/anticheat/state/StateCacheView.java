package net.hollowcube.anticheat.state;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// An immutable view of a [StateCache] at one instant.
///
/// The maps are the cache's own storage, which it copies before its next write, and the accumulate
/// chains are append-only, so taking a view costs nothing and the work only happens if [#frames()]
/// is actually asked for.
public final class StateCacheView {

    private final Map<StateKey, StateFrame> lastWins;
    private final Map<StateKey, FrameNode> accumulated;
    private final EntityTableView entities;
    private final @Nullable String brand;

    StateCacheView(Map<StateKey, StateFrame> lastWins, Map<StateKey, FrameNode> accumulated,
                   EntityTableView entities, @Nullable String brand) {
        this.lastWins = lastWins;
        this.accumulated = accumulated;
        this.entities = entities;
        this.brand = brand;
    }

    public EntityTableView entities() {
        return entities;
    }

    /// The client brand from its `minecraft:brand` payload, when it sent one.
    public @Nullable String brand() {
        return brand;
    }

    public int keyCount() {
        return lastWins.size() + accumulated.size();
    }

    public @Nullable StateFrame frame(StateKey key) {
        return lastWins.get(key);
    }

    /// The frames an accumulate key has collected, oldest first.
    public List<StateFrame> frames(StateKey key) {
        var head = accumulated.get(key);
        return head == null ? List.of() : head.toList();
    }

    /// Every retained frame in the order the cache saw it, which is the order replaying it
    /// reproduces the client's state in.
    public List<StateFrame> frames() {
        var result = new ArrayList<StateFrame>(lastWins.values());
        for (FrameNode head : accumulated.values()) result.addAll(head.toList());
        result.sort(Comparator.comparingLong(StateFrame::sequence));
        return List.copyOf(result);
    }
}
