package net.hollowcube.test.snapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Snapshot<T, S> {

    @NotNull S createSnapshot(@NotNull T value);

    byte @NotNull [] serializeSnapshot(@Nullable S snapshot);

    @Nullable S deserializeSnapshot(byte @NotNull [] serialized);

    void assertSnapshot(S expected, S actual);

}
