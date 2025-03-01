package net.hollowcube.mapmaker.scripting;

import net.hollowcube.mapmaker.scripting.cjs.Module;
import net.hollowcube.mapmaker.scripting.gui.GuiManager;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.hollowcube.mapmaker.scripting.node.Process;
import net.hollowcube.mapmaker.scripting.node.SetTimeout;
import net.hollowcube.mapmaker.scripting.util.Garbage;
import net.minestom.server.MinecraftServer;
import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ScriptEngine is the root of a script execution. It can contain multiple scripts (modules) which
 * have shared global state (such as Symbols in JS).
 *
 * <p>A ScriptEngine maps to a single {@link org.graalvm.polyglot.Context} internally, so limits
 * are applied to an entire script engine not a single script.</p>
 *
 * <p>For scripting in mapmaker we would intend to have one script for global actions, and one
 * script for each player. If we do apply limits and a player engine exceeds those limits the
 * player will be removed from the map. If a global exceeds those limits, the map world itself
 * will be closed with an error.</p>
 */
public class ScriptEngine {
    private static final Logger log = LoggerFactory.getLogger(ScriptEngine.class);
    private final Context context;

    private final Map<URI, Module> moduleCache = new HashMap<>();

    private GuiManager guiManager = null; // Lazy

    public ScriptEngine() {
        this.context = Context.newBuilder().build();

        setupGlobals();
        setupFileWatcher();
    }

    public @NotNull GuiManager guiManager() {
        if (this.guiManager == null)
            this.guiManager = new GuiManager(this);
        return this.guiManager;
    }

    public interface ContextCloser extends AutoCloseable {
        @Override void close();
    }

    public @NotNull Context context() {
        return context;
    }

    /**
     * Enters the context on the current thread, returning a closeable that will leave the
     * context. Intended to be used with try-with-resources expressions.
     *
     * @return a closeable that will leave the context when closed
     */
    public @NotNull ContextCloser makeCurrent() {
        context.enter();
        return context::leave;
    }

    public @NotNull Module load(@NotNull URI script) {
        return load(script, Map.of(), Map.of(), Function.identity());
    }

    public @NotNull Module load(@NotNull URI script, @NotNull Map<String, Object> globals, @NotNull Map<String, Object> extraModules, @NotNull Function<String, String> codeWrapper) {
        if (script.equals(URI.create("internal:///third_party/react/react-refresh/runtime.js")) || script.equals(URI.create("internal:///third_party/react/react-refresh-runtime.js"))) {
            script = URI.create("internal:///third_party/react/react-refresh-runtime.js");
        }
        final Module existing = this.moduleCache.get(script);
        if (existing != null) return existing;
        System.out.println("LOADING AN UNCACHED MODULE " + script);

        final String code = switch (script.getScheme()) {
            case "internal" -> {
                System.out.println("require(" + script + ")");
                //TODO: fix this, we should just support this resolution mechanism probably
                if (script.equals(URI.create("internal:///third_party/react/react-refresh/runtime.js")) || script.equals(URI.create("internal:///third_party/react/react-refresh-runtime.js"))) {
                    script = URI.create("internal:///third_party/react/react-refresh-runtime.js");
                    try {
                        yield Files.readString(Path.of("/Users/matt/dev/javascript/hello-react-custom-renderer/node_modules/react-refresh/cjs/react-refresh-runtime.development.js"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (script.equals(URI.create("internal:///third_party/react/react-reconciler.js"))) {
                    try {
                        yield Files.readString(Path.of("/Users/matt/dev/javascript/hello-react-custom-renderer/node_modules/react-reconciler/cjs/react-reconciler.development.js"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                try (var is = getClass().getResourceAsStream(script.getPath())) {
                    if (is == null) throw new IllegalArgumentException("resource not found: " + script);
                    yield new String(is.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case "guilib" -> {
                try {
                    final Path filePath = Path.of("./guilib/dist/" + script.getPath());
                    yield Files.readString(filePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            case null, default ->
                    throw new UnsupportedOperationException("unsupported uri scheme: " + script.getScheme());
        };
        // TODO: we do not handle circular references here it will be a stack overflow
        final Module loaded = new Module(this, script, codeWrapper.apply(code), globals, extraModules);
        this.moduleCache.put(script, loaded);
        return loaded;
    }

    public @NotNull Module loadText(@NotNull String name, @NotNull String code) {
        var uri = URI.create("file:///tmp/" + name);
        return this.moduleCache.computeIfAbsent(uri, ignored -> new Module(this, uri, code, Map.of(), Map.of()));
    }

    private void setupGlobals() {
        var global = context.getBindings("js");
        global.putMember("process", Process.sandboxedProcess());
        global.putMember("setTimeout", new SetTimeout());
    }

    private void setupFileWatcher() {
        try {
            final Path basePath = Path.of("./guilib/dist");
            var watchService = FileSystems.getDefault().newWatchService();
            WatchKey uncancelledKey = basePath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            //todo we need to cancel the key at some point :(

            Thread.startVirtualThread(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            final Path changedFile = basePath.resolve((Path) event.context());

                            final URI moduleUri = URI.create("guilib:///" + changedFile.toString().replace("./guilib/dist/", ""));
                            System.out.println("FILE CHANGED " + changedFile + " " + moduleUri);

                            var removed = this.moduleCache.remove(moduleUri);
                            if (removed == null) {
                                log.info("module did not exist previously, skipping: {}", moduleUri);
                                continue;
                            }

                            // todo this reloads this module for everyone very cursed mega yikes
                            var newModule = load(moduleUri, removed.globals(), removed.extraModules(), (code) -> {
                                return String.format(Garbage.REACT_REFRESH_MODULE_TEMPLATE, code);
                            });

                            MinecraftServer.getConnectionManager().getOnlinePlayers().forEach(player -> {
                                var host = InventoryHost.forInventory(player.getOpenInventory());
                                if (host == null) return;


                                player.scheduler().scheduleEndOfTick(() -> host.forceRerender(newModule.exports().getMember("default")));
//                                host.queueRedraw();
                            });

                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    log.error("an exception occurred in file watch thread", e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO this entire file watcher API should be abstracted away to a development script loader or something
        //  where we can load from jar which is where it will end up in prod.
    }

}
