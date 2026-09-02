package net.hollowcube.proxy.anticheat;

import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// The anticheat tap needs the player's netty channel, which the velocity api does not expose:
/// only `ConnectedPlayer#getConnection()#getChannel()` on velocity-proxy has it, and velocity-proxy
/// is not published anywhere we can depend on (see the comment in build.gradle.kts). So the three
/// getters are resolved reflectively, once each, against the class of the first receiver they see —
/// there is only ever one: players are `ConnectedPlayer`, their connection a `MinecraftConnection`,
/// the proxy a `VelocityServer`. A proxy that no longer has the method yields `null` rather than an
/// exception — the tap is not installed and the connection is counted as dropped instead of the
/// player being disconnected.
public final class VelocityInternals {
    private static final Getter GET_CONNECTION = new Getter("getConnection");
    private static final Getter GET_CHANNEL = new Getter("getChannel");
    private static final Getter IS_SHUTTING_DOWN = new Getter("isShuttingDown");
    private static final Getter GET_KNOWN_CHANNELS = new Getter("getKnownChannels");

    /// The netty channel behind `player` (a `com.velocitypowered.api.proxy.Player`, taken as an
    /// `Object` so this is testable without a proxy), or null if this build of velocity does not
    /// look like the one we were written against.
    public static @Nullable Channel channelOf(Object player) {
        var connection = GET_CONNECTION.invoke(player);
        if (connection == null) return null;
        return GET_CHANNEL.invoke(connection) instanceof Channel channel ? channel : null;
    }

    /// The plugin channels the client has registered so far — `ConnectedPlayer#getKnownChannels()`,
    /// which the api does not expose — or nothing on a build without it. Registration goes by in
    /// the configuration phase, before the tap is on the pipeline to see it.
    public static Collection<String> knownChannelsOf(Object player) {
        if (!(GET_KNOWN_CHANNELS.invoke(player) instanceof Collection<?> channels)) return List.of();
        var names = new ArrayList<String>(channels.size());
        for (var channel : channels) names.add(String.valueOf(channel));
        return names;
    }

    /// Whether the proxy has begun shutting down. `VelocityServer#shutdown` flips its
    /// `shutdownInProgress` flag before it disconnects anybody and only fires `ProxyShutdownEvent`
    /// afterwards, so this is the one thing that tells a channel closing under a shutdown from a
    /// player leaving. A build without the method reads as not shutting down, which is what every
    /// disconnect looked like before.
    public static boolean isShuttingDown(Object proxyServer) {
        return IS_SHUTTING_DOWN.invoke(proxyServer) instanceof Boolean shuttingDown && shuttingDown;
    }

    /// One reflective getter with a single-entry cache: the receiver class never changes in a
    /// running proxy, so one slot is the whole cache, and a different class (the tests' fakes)
    /// simply resolves again.
    private static final class Getter {
        private record Resolved(Class<?> type, @Nullable MethodHandle handle) {
        }

        private final String name;
        private volatile @Nullable Resolved resolved;

        private Getter(String name) {
            this.name = name;
        }

        @Nullable Object invoke(Object receiver) {
            var resolved = this.resolved;
            if (resolved == null || resolved.type() != receiver.getClass()) {
                resolved = new Resolved(receiver.getClass(), resolve(receiver.getClass()));
                this.resolved = resolved;
            }
            var handle = resolved.handle();
            if (handle == null) return null;
            try {
                return handle.invoke(receiver);
            } catch (Throwable _) {
                return null;
            }
        }

        private @Nullable MethodHandle resolve(Class<?> type) {
            try {
                // Not findVirtual: the return types (MinecraftConnection) are not on our classpath.
                return MethodHandles.lookup().unreflect(type.getMethod(name));
            } catch (ReflectiveOperationException | RuntimeException _) {
                return null;
            }
        }
    }

    private VelocityInternals() {}
}
