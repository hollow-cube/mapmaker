package net.hollowcube.mapmaker.scripting.gui;

import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.scripting.Module;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.gui.react.AtMapmakerGuiModule;
import net.hollowcube.mapmaker.scripting.gui.react.JSX;
import net.hollowcube.mapmaker.scripting.gui.react.ReactRefresh;
import net.hollowcube.mapmaker.scripting.gui.react.ReconcilerHostConfig;
import net.hollowcube.mapmaker.scripting.util.Proxies;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;

/**
 * Manages calls into the react-reconciler runtime itself as well as initial creation of inventory hosts.
 */
@SuppressWarnings("UnstableApiUsage")
public class GuiManager {
    private final ScriptEngine engine;
    public final Module react;
    public final Value reactReconcilerInst;
    private final Value viewSymbol;

    private final Map<String, Object> globals;
    private final Map<String, Object> extraModules;

    public GuiManager(@NotNull ScriptEngine engine) {
        this.engine = engine;

        // React refresh _must_ be initialized before loading React or any of its components.
        if (engine.env().isDevelopment()) {
            ReactRefresh.setup(engine);
        }

        this.react = engine.load(URI.create("internal:///react/react.js"));
        this.reactReconcilerInst = engine.load(URI.create("internal:///react/react-reconciler.js"))
                .exports().execute(new ReconcilerHostConfig(engine));
        if (engine.env().isDevelopment()) {
            // Reconciler dev tools must be loaded for react-refresh to work properly
            this.reactReconcilerInst.invokeMember("injectIntoDevTools");
        }

        this.viewSymbol = engine.makeCurrent(ctx -> ctx.eval("Symbol.for('$$hcView')"));

        this.globals = Map.of("JSX", new JSX(this.react));
        this.extraModules = Map.of("@mapmaker/gui", new AtMapmakerGuiModule(this, this.react));
    }

    /**
     * Opens the GUI at the given module. The module must have a default export of a React component with no props.
     *
     * @param player The player to open the GUI for
     * @param modulePath The module to load
     */
    public void openGui(@NotNull Player player, @NotNull URI modulePath, @NotNull Map<String, Object> extraModules, @NotNull Map<String, Object> props) {
        FutureUtil.assertTickThread(player.acquirable());

        final Map<String, Object> modules = new HashMap<>(this.extraModules);
        modules.putAll(extraModules);
        final Value componentModule = this.engine.load(modulePath, this.globals, modules).exports();
        if (!componentModule.hasMember("default"))
            throw new IllegalArgumentException("Module must have a default export");
        final Value component = componentModule.getMember("default");
        if (!component.canExecute())
            throw new IllegalArgumentException("Default export must be a functional component");
        final InventoryType inventoryType = getInventoryType(component);
        final Value reactElement = this.react.exports().invokeMember("createElement",
                component, Proxies.proxyObject(props));

        final InventoryHost host = new InventoryHost(this, player);

        // todo this is the same as pushView, should just call that prob.
        var renderElement = this.react.exports().invokeMember("createElement",
                this.react.exports().getMember("Fragment"),
                proxyObject(Map.of("key", "0")),
                reactElement);
        host.elements.add(renderElement);

        // Mount and do initial render immediately.
        host.reconcilerRoot = mount(host);
        render(host, inventoryType);
    }

    private @NotNull Value mount(@NotNull InventoryHost host) {
        return reactReconcilerInst.invokeMember("createContainer",
                /* containerInfo */ host,
                /* tag */ 0,
                /* hydrationCallbacks */ null,
                /* isStrictMode */ true,
                /* concurrentUpdatesByDefaultOverride */ null,
                /* identifierPrefix */ "a",
                /* onRecoverableError */ (ProxyExecutable) (args) -> {
                    args[0].as(JSErrorObject.class).getException().printStackTrace();
                    return null;
                },
                /* transitionCallbacks */ null
        );
    }

    public void unmount(@NotNull InventoryHost host) {
        try {
            InventoryHost.CURRENT.set(host);
            reactReconcilerInst.invokeMember("updateContainer",
                    /* element */ null,
                    /* container */ Objects.requireNonNull(host.reconcilerRoot),
                    /* parentComponent */ null,
                    /* callback */ null);
            host.jsExit();
        } finally {
            InventoryHost.CURRENT.remove();
        }
    }

    public @Nullable Inventory render(@NotNull InventoryHost host, @NotNull InventoryType inventoryType) {
        try {
            InventoryHost.CURRENT.set(host);

            var renderElement = this.react.exports().invokeMember("createElement",
                    this.react.exports().getMember("Fragment"),
                    proxyObject(Map.of()),
                    ProxyArray.fromList(host.elements));

            reactReconcilerInst.invokeMember("updateContainer",
                    /* element */ renderElement,
                    /* container */ Objects.requireNonNull(host.reconcilerRoot),
                    /* parentComponent */ null,
                    /* callback */ null);
            host.jsExit();
        } finally {
            InventoryHost.CURRENT.remove();
        }

        // 'Render' the underlying Minestom inventory (items & title)
        return host.drawCurrentElement(inventoryType);
    }

    public @NotNull Value wrapView(@NotNull Value component, @NotNull String inventoryType) {
        component.putMember("$$hcView", viewSymbol);
        component.putMember("inventoryType", inventoryType);
        return component;
    }

    private @NotNull InventoryType getInventoryType(@NotNull Value view) {
        if (!view.getMember("$$hcView").equals(viewSymbol))
            throw new IllegalArgumentException("Views should be created with `view` from `@mapmake/gui`");
        return switch (view.getMember("inventoryType").asString()) {
            case "chest_6_row" -> InventoryType.CHEST_6_ROW;
            case "anvil" -> InventoryType.ANVIL;
            case "cartography" -> InventoryType.CARTOGRAPHY;
            default -> throw new IllegalArgumentException("Unknown inventory type");
        };
    }
}
