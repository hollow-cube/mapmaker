package net.hollowcube.mapmaker.runtime.io;

import net.kyori.adventure.key.Key;

/// One wire from an output of one node to an input of another node (always the activating player right now).
public record Connection(Key input, PlayerInput params) {
}
