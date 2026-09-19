package net.hollowcube.proxy;

import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.hollowcube.ipc.session.GameServer;
import net.hollowcube.ipc.session.SessionService;
import net.hollowcube.ipc.util.IpcException;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendsTest {

    private final Deque<Supplier<@Nullable GameServer>> answers = new ArrayDeque<>();
    private final List<@Nullable String> excludes = new ArrayList<>();
    private final Backends backends = new Backends(LoggerFactory.getLogger(BackendsTest.class), fakeProxy(),
        new SessionService() {
            @Override
            public int onlinePlayers() {
                return 0;
            }

            @Override
            public @Nullable GameServer findHub(@Nullable String exclude) {
                excludes.add(exclude);
                return answers.removeFirst().get();
            }

            @Override
            public @Nullable GameServer findServer(String id) {
                excludes.add(id);
                return answers.removeFirst().get();
            }
        });

    @AfterEach
    void close() {
        backends.close();
    }

    @Test
    void target_readsTheJsonABackendSends() {
        var data = "{\"server\":\"hub-a\",\"address\":\"10.42.0.7\",\"protocolVersion\":777}".getBytes(StandardCharsets.UTF_8);
        assertEquals(new Backends.Target("hub-a", "10.42.0.7", 777), Backends.Target.parse(data));
    }

    @Test
    void target_takesABareAddressFromAnOlderBackendAsUnknownProtocol() {
        var data = "10.42.0.7".getBytes(StandardCharsets.UTF_8);
        assertEquals(new Backends.Target(null, "10.42.0.7", 0), Backends.Target.parse(data));
    }

    @Test
    void serverName_isStablePerAddressAndHidesIt() {
        var name = Backends.serverName("10.42.0.7");
        assertEquals(name, Backends.serverName("10.42.0.7"));
        assertNotEquals(name, Backends.serverName("10.42.0.8"));
        assertTrue(name.matches("hc-[0-9a-f]{8}"), name);
    }

    @Test
    void findHub_remembersWhichHubItFound() {
        answers.add(() -> new GameServer("hub-a", "10.42.0.7", 777));

        var hub = backends.findHub(null).join();
        assertNotNull(hub);
        assertEquals(Backends.serverName("10.42.0.7"), hub.getServerInfo().getName());
        assertEquals("hub-a", backends.hubId(hub.getServerInfo().getName()));
    }

    @Test
    void findHub_retriesWhileNoneIsReadyOrTheApiIsDown() {
        answers.add(() -> null);
        answers.add(() -> {
            throw new IpcException(0, "connection refused");
        });
        answers.add(() -> {
            throw new IpcException(503, "draining");
        });
        answers.add(() -> new GameServer("hub-b", "10.42.0.8", 777));

        assertNotNull(backends.findHub("hub-a").join());
        assertEquals(List.of("hub-a", "hub-a", "hub-a", "hub-a"), excludes);
    }

    @Test
    void findHub_givesUpOnAnErrorRetryingWillNotFix() {
        answers.add(() -> {
            throw new IpcException(400, "bad request");
        });

        var error = assertThrows(CompletionException.class, () -> backends.findHub(null).join());
        assertInstanceOf(IpcException.class, error.getCause().getCause());
        assertEquals(1, excludes.size());
    }

    @Test
    void findHub_isNullOnceTheBudgetIsSpent() {
        for (int i = 0; i < 100; i++) answers.add(() -> null);

        assertNull(backends.findHub(null).join());
    }

    @Test
    void findServer_isNullAtOnceForAServerThatIsGone() {
        answers.add(() -> null);

        assertNull(backends.findServer("isolate-a").join());
        assertEquals(List.of("isolate-a"), excludes);
    }

    @Test
    void findServer_retriesWhileTheApiIsDown() {
        answers.add(() -> {
            throw new IpcException(0, "connection refused");
        });
        answers.add(() -> new GameServer("isolate-a", "10.42.0.9", 777));

        var server = backends.findServer("isolate-a").join();
        assertNotNull(server);
        assertEquals(Backends.serverName("10.42.0.9"), server.getServerInfo().getName());
    }

    private static ProxyServer fakeProxy() {
        var plugins = (PluginManager) Proxy.newProxyInstance(BackendsTest.class.getClassLoader(),
            new Class<?>[]{PluginManager.class}, (_, method, _) -> switch (method.getName()) {
                case "isLoaded" -> false;
                default -> throw new UnsupportedOperationException(method.getName());
            });
        return (ProxyServer) Proxy.newProxyInstance(BackendsTest.class.getClassLoader(),
            new Class<?>[]{ProxyServer.class}, (_, method, args) -> switch (method.getName()) {
                case "getPluginManager" -> plugins;
                case "createRawRegisteredServer" -> fakeServer((ServerInfo) args[0]);
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private static RegisteredServer fakeServer(ServerInfo info) {
        return (RegisteredServer) Proxy.newProxyInstance(BackendsTest.class.getClassLoader(),
            new Class<?>[]{RegisteredServer.class}, (_, method, _) -> switch (method.getName()) {
                case "getServerInfo" -> info;
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
