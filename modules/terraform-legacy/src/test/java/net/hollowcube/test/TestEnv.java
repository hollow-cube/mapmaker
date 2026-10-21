package net.hollowcube.test;

import net.hollowcube.test.snapshot.Snapshot;
import net.hollowcube.test.subject.TestConnection;
import net.hollowcube.test.subject.TestInstance;
import net.hollowcube.test.subject.TestPlayer;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public interface TestEnv {

    @NotNull ServerProcess process();

    @NotNull TestInstance createEmptyInstance();

    @NotNull TestConnection createConnection();

    @NotNull TestPlayer createPlayer(@NotNull Instance instance, @NotNull Pos pos);


    // Snapshot tests

    <T, S> void assertSnapshot(@NotNull Class<? extends Snapshot<T, S>> snapshotType, @NotNull T value);

}
