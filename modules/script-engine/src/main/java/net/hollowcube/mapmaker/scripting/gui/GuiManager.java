package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.Module;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.gui.react.AtMapmakerGuiModule;
import net.hollowcube.mapmaker.scripting.gui.react.JSX;
import net.hollowcube.mapmaker.scripting.gui.react.ReactRefresh;
import net.hollowcube.mapmaker.scripting.gui.react.ReconcilerHostConfig;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;

public class GuiManager {
    private final ScriptEngine engine;
    private final Module react;
    private final Value reactReconcilerInst;
    private final Value hostContext;
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
                .exports().execute(new ReconcilerHostConfig());
        if (engine.env().isDevelopment()) {
            // Reconciler dev tools must be loaded for react-refresh to work properly
            this.reactReconcilerInst.invokeMember("injectIntoDevTools");
        }

        this.hostContext = this.react.exports().invokeMember("createContext");
        this.react.exports().putMember("__hollowcube_hostContext", this.hostContext);

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
    public void openGui(@NotNull Player player, @NotNull URI modulePath) {
        final Value componentModule = this.engine.load(modulePath, this.globals, this.extraModules).exports();
        if (!componentModule.hasMember("default"))
            throw new IllegalArgumentException("Module must have a default export");
        final Value component = componentModule.getMember("default");
        if (!component.canExecute())
            throw new IllegalArgumentException("Default export must be a functional component");
        final InventoryType inventoryType = getInventoryType(component);
        final Value reactElement = this.react.exports().invokeMember("createElement", component, null);

        final InventoryHost host = new InventoryHost(this, player);

        // Wrap the element in a context provider
        final Value contextProviderElement = this.react.exports().invokeMember("createElement",
                this.hostContext.getMember("Provider"),
                proxyObject(Map.of("value", host)),
                reactElement);

        // Mount and do initial render immediately.
        host.reconcilerRoot = mount(host);
        render(host, contextProviderElement, inventoryType);

        player.openInventory(host.handle());
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
                    System.out.println("ERROR: " + args[0]);
                    return null;
                },
                /* transitionCallbacks */ null
        );
    }

    public void unmount(@NotNull InventoryHost host) {
        reactReconcilerInst.invokeMember("updateContainer",
                /* element */ null,
                /* container */ Objects.requireNonNull(host.reconcilerRoot),
                /* parentComponent */ null,
                /* callback */ null);
    }

    public void render(@NotNull InventoryHost host, @NotNull Value reactElement, @NotNull InventoryType inventoryType) {
        reactReconcilerInst.invokeMember("updateContainer",
                /* element */ reactElement,
                /* container */ Objects.requireNonNull(host.reconcilerRoot),
                /* parentComponent */ null,
                /* callback */ null);

        // 'Render' the underlying Minestom inventory (items & title)
        host.drawCurrentElement(inventoryType);
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
