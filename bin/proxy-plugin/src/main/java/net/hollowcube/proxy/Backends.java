package net.hollowcube.proxy;

import com.google.gson.JsonParser;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.hollowcube.ipc.session.GameServer;
import net.hollowcube.ipc.session.SessionService;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/// The backends players are connected to. None are configured in velocity.toml: each one is a pod
/// the session service named, created raw on the way to it.
///
/// ViaVersion picks the protocol it translates to by the server's name, so every backend is named
/// for its address and its protocol is registered under that name before the connect. That is what
/// lets a rollout leave the fleet on two Minecraft versions without restarting the proxy.
final class Backends {
    private static final int MINECRAFT_PORT = 25565;

    private static final Duration LOOKUP_ATTEMPT_TIMEOUT = Duration.ofSeconds(2);
    // A hub rollout can leave none ready for a moment: the replacement is only offered once the
    // tracker's 5s pod sync has seen it, so this covers a couple of syncs.
    private static final Duration LOOKUP_BUDGET = Duration.ofSeconds(12);
    private static final Duration LOOKUP_RETRY_DELAY = Duration.ofMillis(500);

    /// A place to send a player: its `server_states` id, where it is and what it speaks, 0 when
    /// that is unknown.
    ///
    /// @param server null from a backend on a build that only sends the address
    record Target(@Nullable String server, String address, int protocolVersion) {

        /// `mapmaker:transfer` is json since backends learned the target's protocol. A backend
        /// still on an older build sends the bare address.
        static Target parse(byte[] data) {
            var text = new String(data, StandardCharsets.UTF_8);
            if (!text.startsWith("{")) return new Target(null, text, 0);
            var json = JsonParser.parseString(text).getAsJsonObject();
            var server = json.get("server");
            var protocolVersion = json.get("protocolVersion");
            return new Target(
                server == null || server.isJsonNull() ? null : server.getAsString(),
                json.get("address").getAsString(),
                protocolVersion == null || protocolVersion.isJsonNull() ? 0 : protocolVersion.getAsInt());
        }
    }

    private final Logger logger;
    private final ProxyServer proxy;
    private final SessionService sessions;
    private final boolean via;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Server name to hub pod id, so a player kicked off a hub is sent to any hub but that one.
    private final Map<String, String> hubIds = new ConcurrentHashMap<>();

    Backends(Logger logger, ProxyServer proxy, SessionService sessions) {
        this.logger = logger;
        this.proxy = proxy;
        this.sessions = sessions;
        this.via = proxy.getPluginManager().isLoaded("viaversion");
        if (!via) logger.warn("viaversion is not loaded, backends are connected to in the player's protocol");
    }

    RegisteredServer server(Target target) {
        var name = serverName(target.address());
        if (via) ViaProtocols.register(name, target.protocolVersion());
        return proxy.createRawRegisteredServer(new ServerInfo(name, new InetSocketAddress(target.address(), MINECRAFT_PORT)));
    }

    /// The pod id of the hub `serverName` names, or null when it is not a hub this proxy found.
    @Nullable String hubId(String serverName) {
        return hubIds.get(serverName);
    }

    /// A ready hub other than the pod `exclude`, retried through the errors a restarting api-server
    /// or hub rollout produces. Completes with null when there is still none once the budget is
    /// spent, and exceptionally for anything retrying will not fix.
    CompletableFuture<@Nullable RegisteredServer> findHub(@Nullable String exclude) {
        return lookup("a hub", () -> sessions.findHub(exclude), true).thenApply(hub -> {
            if (hub == null) return null;
            var server = server(new Target(hub.id(), hub.clusterIp(), hub.protocolVersion()));
            hubIds.put(server.getServerInfo().getName(), hub.id());
            return server;
        });
    }

    /// The server `id` names, or null when its pod is gone.
    CompletableFuture<@Nullable RegisteredServer> findServer(String id) {
        return lookup("server " + id, () -> sessions.findServer(id), false).thenApply(found ->
            found == null ? null : server(new Target(found.id(), found.clusterIp(), found.protocolVersion())));
    }

    /// @param retryNull whether null means "not yet" rather than an answer
    private CompletableFuture<@Nullable GameServer> lookup(String what, Callable<@Nullable GameServer> call, boolean retryNull) {
        return CompletableFuture.supplyAsync(() -> {
            long deadline = System.nanoTime() + LOOKUP_BUDGET.toNanos();
            while (true) {
                try {
                    var found = executor.submit(call).get(LOOKUP_ATTEMPT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                    if (found != null || !retryNull) return found;
                    logger.warn("no {} is ready", what);
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof IpcException ipc) || !retryable(ipc))
                        throw new IllegalStateException("failed to find " + what, e.getCause());
                    logger.warn("failed to find {}, retrying: {}", what, ipc.getMessage());
                } catch (TimeoutException e) {
                    logger.warn("finding {} took over {}, retrying", what, LOOKUP_ATTEMPT_TIMEOUT);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted finding " + what, e);
                }

                if (System.nanoTime() + LOOKUP_RETRY_DELAY.toNanos() - deadline >= 0) return null;
                try {
                    Thread.sleep(LOOKUP_RETRY_DELAY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("interrupted finding " + what, e);
                }
            }
        }, executor);
    }

    void close() {
        executor.shutdownNow();
    }

    /// Status 0 is the request never getting an answer.
    private static boolean retryable(IpcException e) {
        return e.status() == 0 || e.status() >= 500;
    }

    /// Velocity shows the name to players ("Unable to connect you to ..."), so it is not the pod ip.
    static String serverName(String address) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(address.getBytes(StandardCharsets.UTF_8));
            return "hc-" + HexFormat.of().formatHex(digest, 0, 4);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
