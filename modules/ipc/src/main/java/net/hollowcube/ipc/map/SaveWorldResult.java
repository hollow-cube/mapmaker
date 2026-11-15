package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

public sealed interface SaveWorldResult {

    record Saved() implements SaveWorldResult {}

    record NotHeld() implements SaveWorldResult {}

    record Unknown(@Nullable String type) implements SaveWorldResult {}
}
