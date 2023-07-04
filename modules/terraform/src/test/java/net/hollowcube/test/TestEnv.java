package net.hollowcube.test;

import net.minestom.server.ServerProcess;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public interface TestEnv {

    @NotNull ServerProcess process();

    @NotNull Instance createEmptyInstance();

}
