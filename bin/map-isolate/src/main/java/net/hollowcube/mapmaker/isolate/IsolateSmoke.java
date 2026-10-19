package net.hollowcube.mapmaker.isolate;

import com.google.gson.JsonParser;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.compiler.LuauCompiler;
import net.hollowcube.luau.gen.runtime.GeneratedStringAtoms;
import net.hollowcube.luau.gen.runtime.LuaApiRegistry;
import net.hollowcube.mapmaker.config.*;
import net.hollowcube.mapmaker.map.ReadableMapData;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.runtime.MapServerInitializer;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.scripting.api.LuaGlobals;
import net.hollowcube.mapmaker.scripting.api.LuaVector;
import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.Channels;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/// Exercises the paths that need native-image metadata (zstd/polar, luau, gson, networking) without
/// the api server or nats, so CI can run the shipped image before pushing it. A missing registration
/// either throws here or is printed as a Missing*RegistrationError, which CI fails on.
final class IsolateSmoke {

    static int run() {
        var probes = List.<Probe>of(
            new Probe("config", IsolateSmoke::config),
            new Probe("world", IsolateSmoke::world),
            new Probe("scripting", IsolateSmoke::scripting),
            new Probe("status", IsolateSmoke::status)
        );

        int failed = 0;
        for (var probe : probes) {
            var result = new CompletableFuture<@Nullable Void>();
            FutureUtil.submitVirtual(() -> {
                try {
                    probe.body.run();
                    result.complete(null);
                } catch (Throwable t) {
                    result.completeExceptionally(t);
                }
            });
            try {
                result.get(60, TimeUnit.SECONDS);
                System.out.println("smoke: " + probe.name + " ok");
            } catch (Throwable t) {
                failed++;
                System.out.println("smoke: " + probe.name + " FAILED");
                (t instanceof ExecutionException e ? e.getCause() : t).printStackTrace(System.out);
            }
        }
        return failed == 0 ? 0 : 1;
    }

    private static void config() {
        var config = ConfigLoaderV3.loadDefault(new String[0]);
        for (var type : List.of(GlobalConfig.class, HttpConfig.class, Ipc_ServiceConfig.class, Map_ServiceConfig.class,
            MetricsConfig.class, MinestomConfig.class, Player_ServiceConfig.class, Session_ServiceConfig.class,
            SpiceDBConfig.class, TracingConfig.class, VelocityConfig.class)) {
            check(config.get(type) != null, "missing config " + type.getSimpleName());
        }
        check(config.get(HttpConfig.class).port() != 0, "HttpConfig deserialized empty");
    }

    private static void world() throws Exception {
        ParkourMapWorld.initGlobalReferences();

        var source = new MapInstance("smoke", MapInstance.LightingMode.FULL_BRIGHT);
        for (int x = -24; x < 24; x++) {
            for (int z = -24; z < 24; z++) {
                source.loadChunk(x >> 4, z >> 4).join();
                source.setBlock(x, 64, z, (x + z) % 2 == 0 ? Block.STONE : Block.OAK_PLANKS);
            }
        }
        source.setBlock(0, 65, 0, Block.CHEST);
        source.setBlock(1, 65, 0, Block.OAK_SIGN);
        byte[] saved = source.save(new PolarWorldAccess() {
            @Override
            public void saveWorldData(net.minestom.server.instance.Instance instance, NetworkBuffer buffer) {
            }
        });

        var loaded = new MapInstance("smoke-loaded", MapInstance.LightingMode.FULL_BRIGHT);
        loaded.loadStream(new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(saved)), saved.length), null);
        check(loaded.getBlock(3, 64, 0).compare(Block.OAK_PLANKS), "block did not round trip");
        check(loaded.getBlock(0, 65, 0).compare(Block.CHEST), "chest did not round trip");

        MinecraftServer.getInstanceManager().unregisterInstance(source);
        MinecraftServer.getInstanceManager().unregisterInstance(loaded);
    }

    private static void scripting() throws Exception {
        var bytecode = LuauCompiler.DEFAULT.compile("""
            local parts = {}
            for i = 1, 3 do table.insert(parts, string.format("%d", i * 2)) end
            return #table.concat(parts, ",")
            """);

        var state = LuaState.newState();
        try {
            state.openLibs();
            LuaGlobals.register(state);
            LuaVector.register(state);
            GeneratedStringAtoms.register(state);
            LuaApiRegistry.register(state);
            state.sandbox();

            var thread = state.newThread();
            thread.sandboxThread();
            thread.load("/smoke", bytecode);
            thread.call(0, 1);
            check(thread.toInteger(-1) == 5, "unexpected script result " + thread.toInteger(-1));
        } finally {
            state.close();
        }
    }

    private static void status() throws Exception {
        int port;
        try (var socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        MapServerInitializer.preInitializedServer.start("127.0.0.1", port);

        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
            socket.setSoTimeout(5000);

            var handshake = NetworkBuffer.makeArray(buffer -> {
                buffer.write(NetworkBuffer.VAR_INT, 0);
                buffer.write(NetworkBuffer.VAR_INT, MinecraftServer.PROTOCOL_VERSION);
                buffer.write(NetworkBuffer.STRING, "127.0.0.1");
                buffer.write(NetworkBuffer.UNSIGNED_SHORT, port);
                buffer.write(NetworkBuffer.VAR_INT, 1);
            });
            var out = socket.getOutputStream();
            out.write(NetworkBuffer.makeArray(buffer -> {
                buffer.write(NetworkBuffer.VAR_INT, handshake.length);
                buffer.write(NetworkBuffer.RAW_BYTES, handshake);
                buffer.write(NetworkBuffer.VAR_INT, 1);
                buffer.write(NetworkBuffer.VAR_INT, 0);
            }));
            out.flush();

            var in = new DataInputStream(socket.getInputStream());
            var packet = new byte[readVarInt(in)];
            in.readFully(packet);
            var buffer = NetworkBuffer.wrap(packet, 0, packet.length);
            check(buffer.read(NetworkBuffer.VAR_INT) == 0, "unexpected status packet id");
            var json = JsonParser.parseString(buffer.read(NetworkBuffer.STRING)).getAsJsonObject();
            check(json.getAsJsonObject("version").get("protocol").getAsInt() == MinecraftServer.PROTOCOL_VERSION,
                "unexpected status response " + json);
        } finally {
            MinecraftServer.stopCleanly();
        }
    }

    private static int readVarInt(DataInputStream in) throws Exception {
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return value;
        }
        throw new IllegalStateException("varint too long");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private interface Body {
        void run() throws Exception;
    }

    private record Probe(String name, Body body) {
    }
}
