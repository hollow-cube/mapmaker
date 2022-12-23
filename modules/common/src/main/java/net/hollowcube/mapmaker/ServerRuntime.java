package net.hollowcube.mapmaker;

import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;

public interface ServerRuntime {

    // Server info

    default @NotNull String version() { return "unknown"; }
    default @NotNull String commit() { return "dev"; }

    /** Returns some identifier of the current instance. For example in a Kubernetes environment the pod name would work. */
    default @NotNull String workerId() { return "dev"; }

    // Dependency info

    default @NotNull String minestom() { return "unknown"; }

    /** Gets the current runtime. Server binaries must implement this interface through the SPI mechanism. */
    static @NotNull ServerRuntime getRuntime() {
        // Cursed yikes code, but it does work :)
        // I wanted an excuse to use this weird feature of classes inside methods, and it does make a clean api here.
        class Holder {
            static ServerRuntime runtime = null;
        }
        if (Holder.runtime == null) {
            Holder.runtime = ServiceLoader.load(ServerRuntime.class)
                    .findFirst().orElseGet(() -> new ServerRuntime(){});
        }
        return Holder.runtime;
    }
}
