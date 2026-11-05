package net.hollowcube.mapmaker.runtime.io;

import net.kyori.adventure.key.Key;

/// One wire from an output of one node to an input of another node (always the activating player right now).
///
/// @param output the source node's output this fires on
public record Connection(Key output, Key input, PlayerInput params) {}
