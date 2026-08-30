package net.hollowcube.proxy.anticheat;

import com.sun.net.httpserver.HttpServer;
import net.hollowcube.ipc.Blob;
import net.hollowcube.ipc.Wire;
import net.hollowcube.ipc.anticheat.AnticheatServer;
import net.hollowcube.ipc.anticheat.AnticheatService;
import net.hollowcube.ipc.anticheat.PutResult;
import net.hollowcube.ipc.anticheat.TraceMeta;
import net.hollowcube.ipc.anticheat.TraceRow;
import net.hollowcube.ipc.util.IpcException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/// The api-server's trace store, minus the api-server: the generated [AnticheatServer]
/// over a directory of blobs. What the shipper actually talks to, so its retries and its refusals
/// are the real ones over a real socket.
///
/// Blobs land flat as `{id}.trace`, which is what the e2e harness's `collect` walks. Every put is
/// stored and answered unless a status has been queued, which is how a store that is down for one
/// attempt and up for the next is written, and every put prints the meta it was filed under, so a
/// trace can be read about without opening it.
///
/// [#main] is the same store on a port, for dev and for the harness.
final class StubStore implements AnticheatService, AutoCloseable {

    private static final String EXTENSION = ".trace";

    record Put(TraceMeta meta, long bytes) {
    }

    private final HttpServer server;
    private final ExecutorService threads =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("stub-store-", 0).factory());
    private final Path root;

    private final Queue<Integer> statuses = new ConcurrentLinkedQueue<>();
    private final List<Put> puts = new CopyOnWriteArrayList<>();
    private final Map<String, TraceRow> rows = new ConcurrentHashMap<>();

    private volatile Duration delay = Duration.ZERO;

    /// @param port 0 for whatever the os hands out, which is what a test wants
    static StubStore start(Path root, int port) {
        try {
            Files.createDirectories(root);
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            var store = new StubStore(server, root);
            server.createContext(AnticheatServer.PATH, new AnticheatServer(store));
            server.setExecutor(store.threads);
            server.start();
            return store;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private StubStore(HttpServer server, Path root) {
        this.server = server;
        this.root = root;
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    List<Put> puts() {
        return puts;
    }

    /// The answers to the next puts, in order; anything after them is stored.
    void answer(int... statuses) {
        for (var status : statuses) this.statuses.add(status);
    }

    /// How long the store takes to answer, for a shutdown that has to give up on it.
    void delay(Duration delay) {
        this.delay = delay;
    }

    @Override
    public PutResult putTrace(TraceMeta meta, Blob body) {
        var path = root.resolve(meta.id() + EXTENSION);
        var part = root.resolve(meta.id() + ".part");
        var replaced = Files.exists(path);
        long bytes;
        try (body) {
            bytes = Files.copy(body.stream(), part, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        puts.add(new Put(meta, bytes));
        sleep(delay);

        var status = statuses.poll();
        if (status != null && status >= 400) {
            delete(part);
            throw new IpcException(status, "answering " + status + " on purpose");
        }
        try {
            Files.move(part, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var stored = new PutResult(path.getFileName().toString(), bytes, replaced);
        rows.put(meta.id(), new TraceRow(meta, bytes, stored.path(), false, null, System.currentTimeMillis()));
        System.out.println((replaced ? "re-stored " : "stored ") + meta.id() + " (" + bytes + " bytes)");
        System.out.println("  " + Wire.gson().toJson(meta));
        return stored;
    }

    @Override
    public Blob getTrace(String id) {
        var path = root.resolve(id + EXTENSION);
        if (!rows.containsKey(id) || !Files.isRegularFile(path)) throw new IpcException(404, "no trace " + id);
        try {
            return Blob.of(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<TraceRow> listTraces(String captureId) {
        return rows.values().stream()
            .filter(row -> captureId.equals(row.meta().captureId()))
            .sorted(Comparator.comparingLong(TraceRow::createdAt))
            .toList();
    }

    @Override
    public void close() {
        server.stop(0);
        threads.shutdownNow();
    }

    /// The store a dev proxy points `ANTICHEAT_STORE_URL` at by default, for when the real one is
    /// not on the host or is an api-server build that predates the service; 9126 rather than 9127
    /// because docker already publishes the api-server's 9124 there.
    ///
    ///     ./gradlew :bin:proxy-plugin:runStubStore -Pport=9126 -Pdir=scratch/proxy/store
    ///
    /// Dump one of the blobs with `./gradlew :modules:anticheat:dumpTrace -Pfile=<blob>`.
    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.println("usage: stub-store <port> <dir>");
            System.exit(2);
        }
        var port = Integer.parseInt(args[0]);
        var root = Path.of(args[1]).toAbsolutePath();
        start(root, port);
        System.out.println("stub store on http://127.0.0.1:" + port + AnticheatServer.PATH
            + ", writing to " + root);
        // Nothing ever counts this down: the store runs until whoever started it stops it.
        new CountDownLatch(1).await();
    }

    private static void delete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// Interrupted means the store is closing: abort the put rather than racing the test's
    /// temp-directory cleanup with a file move.
    private static void sleep(Duration duration) {
        if (duration.isZero()) return;
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IpcException(503, "store closing");
        }
    }
}
