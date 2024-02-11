package net.hollowcube.common;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ServiceLoader;
import java.util.concurrent.ThreadLocalRandom;

public interface ServerRuntime {

    default boolean isDevelopment() {
        return commit().equals("dev");
    }

    // Server info

    default @NotNull String version() {
        return "unknown";
    }

    default @NotNull String commit() {
        return "dev";
    }

    default @NotNull String shortCommit() {
        if (commit().length() < 7)
            return commit();
        return commit().substring(0, 7);
    }

    /**
     * Returns some identifier of the current instance.
     * For example in a Kubernetes environment the pod name would work.
     *
     * @implNote The default value is a consistent random string.
     * In a deployment it should be overridden with a real hostname value
     */
    default @NotNull String hostname() {
        class Holder {
            static String hostname = null;
        }
        if (Holder.hostname == null) {
            try {
                Holder.hostname = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }
        return Holder.hostname;
    }


    default @NotNull String resourcePackSha1() {
        return "dev";
    }

    // Dependency info

    default @NotNull String minestom() {
        return "unknown";
    }

    /**
     * Gets the current runtime. Server binaries must implement this interface through the SPI mechanism.
     */
    static @NotNull ServerRuntime getRuntime() {
        // Cursed yikes code, but it does work :)
        // I wanted an excuse to use this weird feature of classes inside methods, and it does make a clean api here.
        class Holder {
            static ServerRuntime runtime = null;
        }
        if (Holder.runtime == null) {
            Holder.runtime = ServiceLoader.load(ServerRuntime.class)
                    .findFirst().orElseGet(() -> new ServerRuntime() {
                    });
        }
        return Holder.runtime;
    }
}
