package net.hollowcube.mapmaker.dev.inventory;

import net.hollowcube.mapmaker.dev.element.Node;
import net.hollowcube.mapmaker.dev.jsx.JSX;
import net.hollowcube.mapmaker.dev.render.RenderContext;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class InventoryHost {
    static {
        System.setProperty("polyglot.engine.WarnInterpreterOnly", "false");
    }

    // TODO eventually the scripts should be loaded with some kind of manager which deals with file
    //  watching, notifying on change, etc. Potentially could even just manage modules entirely and
    //  deal with invalidating the proper dependency tree when one reloads.
    private static final Path BASE_PATH;

    static {
        try {
            BASE_PATH = Path.of("./guilib/dist").toRealPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final long load = System.currentTimeMillis();
    private final Player player;
    private final String entrypoint;

    private List<Context> contexts = new ArrayList<>();
    private Inventory inventory;

    public InventoryHost(@NotNull Player player, @NotNull String entrypoint) {
        this.player = player;
        this.entrypoint = entrypoint;

        this.beginWatching();
        this.loadScriptedGui();
    }

    private void loadScriptedGui() {
        if (!contexts.isEmpty()) {
            player.sendMessage("reload " + NumberUtil.formatPlayerPlaytime(System.currentTimeMillis() - load));
        }

        contexts.forEach(Context::close);
        contexts.clear();

        var modules = loadSources();

        AtomicReference<ProxyExecutable> requireRef = new AtomicReference<>();
        requireRef.set(arguments -> {
            if (arguments.length != 1)
                throw new IllegalArgumentException("expected 1 argument, got " + arguments.length);
            var path = BASE_PATH.relativize(BASE_PATH.resolve(arguments[0].asString())).toString();

            Context context = Context.newBuilder("js").build();
            contexts.add(context);
            var js = context.getBindings("js");
            js.putMember("require", requireRef.get());
            js.putMember("JSX", JSX.INSTANCE);
            var module = context.eval("js", "({})");
            js.putMember("exports", module);

            context.eval(modules.get(path));
            return module;
        });

        try (Context context = Context.newBuilder("js")
                .build()) {
            var js = context.getBindings("js");
            js.putMember("require", requireRef.get());
            js.putMember("JSX", JSX.INSTANCE);

            var finalResult = context.eval("js", "JSX.createElement(require('" + entrypoint + "').default, null)");

            var rc = new RenderContext(9, 6);
            ((Node) finalResult.asHostObject()).render(rc);

            var inventory = new Inventory(InventoryType.CHEST_6_ROW, rc.getTitleText());
            var items = rc.getItems();
            for (int i = 0; i < items.length; i++) {
                inventory.setItemStack(i, items[i]);
            }

            player.openInventory(inventory);
        }

    }

    private @NotNull Map<String, Source> loadSources() {
        try {
            List<Path> files;
            try (var stream = Files.walk(BASE_PATH)) {
                files = stream.toList();
            }

            var modules = new HashMap<String, Source>();
            for (var path : files) {
                if (Files.isDirectory(path)) continue;
                if (!path.toString().endsWith(".js")) continue;

                var name = BASE_PATH.relativize(path).toString().replace(".js", "");
                var source = Source.newBuilder("js", path.toUri().toURL())
                        .encoding(StandardCharsets.UTF_8)
                        .content(Files.readString(path))
                        .build();
                modules.put(name, source);
            }

            return modules;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void beginWatching() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();

            // Watch everything, we will reload everything on change for now.
            BASE_PATH.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_CREATE);

            Thread.startVirtualThread(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        // DNC about the events, we just reload everything
                        player.scheduleNextTick(ignored -> loadScriptedGui());


                        for (WatchEvent<?> event : key.pollEvents()) {
                        }
                        key.reset();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
