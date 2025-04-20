package net.hollowcube.mapmaker.scripting.gui;

import com.oracle.truffle.js.runtime.builtins.JSErrorObject;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.scripting.Module;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.annotation.ScriptSafe;
import net.hollowcube.mapmaker.scripting.gui.react.AtMapmakerGuiModule;
import net.hollowcube.mapmaker.scripting.gui.react.JSX;
import net.hollowcube.mapmaker.scripting.gui.react.ReactRefresh;
import net.hollowcube.mapmaker.scripting.gui.react.ReconcilerHostConfig;
import net.hollowcube.mapmaker.scripting.util.Proxies;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Manages calls into the react-reconciler runtime itself as well as initial creation of inventory hosts.
 */
@SuppressWarnings("UnstableApiUsage")
public class GuiManager {
    private final ScriptEngine engine;
    private final Module react;
    private final Value reactReconcilerInst;
    private final Value viewSymbol;
    private final Value elementSymbol;
    private final Value fragmentSymbol;

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

        // TODO: Why do we make a symbol? We can just use a java object which cant be created from JS.
        this.viewSymbol = engine.makeCurrent(ctx -> ctx.eval("Symbol.for('$$hcView')"));
        this.elementSymbol = engine.makeCurrent(ctx -> ctx.eval("Symbol.for('react.transitional.element')"));
        this.fragmentSymbol = react.exports().getMember("Fragment");

        this.globals = Map.of("JSX", new JSX(this.react));
        this.extraModules = Map.of("@mapmaker/gui", new AtMapmakerGuiModule(this, this.react));
    }

    public @NotNull ScriptEngine engine() {
        return this.engine;
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

        // Since we are mounting a module we need to make a component out of it. This is equivalent to writing
        // `<theModule.default {...props} />` in JSX, which is what you would do in a pushView call and what
        // InventoryHost#pushView expects to receive. Note that the default export must be wrapped in a view() call.
        final Value componentModule = this.engine.load(modulePath, this.globals, modules).exports();
        if (!componentModule.hasMember("default"))
            throw new IllegalArgumentException("Module must have a default export");
        final Value component = componentModule.getMember("default");
        if (!component.canExecute())
            throw new IllegalArgumentException("Default export must be a functional component");
        // Small note: props must be backed by a mutable map because we insert into the props map in InventoryHost.
        final Value reactElement = reactCreateElement(component, Proxies.proxyObject(new HashMap<>(props)), null);

        // Create and mount the new host as a react root
        final InventoryHost host = new InventoryHost(this, player);
        createContainer(host);

        // Push the initial view and render it
        host.pushView(reactElement);
    }

    /**
     * Mounts the given host as a React root. This may not be called multiple times.
     */
    void createContainer(@NotNull InventoryHost host) {
        if (host.reconcilerRoot != null)
            throw new IllegalStateException("Host is already mounted");
        host.reconcilerRoot = reactReconcilerInst.invokeMember("createContainer",
                /* containerInfo */ host,
                /* tag */ 0,
                /* hydrationCallbacks */ null,
                /* isStrictMode */ true,
                /* concurrentUpdatesByDefaultOverride */ null,
                /* identifierPrefix */ "a",
                /* onRecoverableError */ (ProxyExecutable) (args) -> {
                    // TODO: we should recover from this :)
                    args[0].as(JSErrorObject.class).getException().printStackTrace();
                    return null;
                },
                /* transitionCallbacks */ null
        );
    }

    /**
     * Updates the root container with the given React element. This may only be called after {@link #createContainer(InventoryHost)}.
     * @param host The host to update
     * @param reactElement The new React element to render. This may be null to unmount the host.
     */
    void updateContainer(@NotNull InventoryHost host, @Nullable Object reactElement) {
        reactReconcilerInst.invokeMember("updateContainer",
                /* element */ reactElement,
                /* container */ Objects.requireNonNull(host.reconcilerRoot),
                /* parentComponent */ null,
                /* callback */ null);
    }

    @NotNull Value reactCreateElement(@NotNull Value component, @Nullable ProxyObject props, @Nullable List<Object> children) {
        if (children != null) {
            return this.react.exports().invokeMember("createElement", component,
                    props, ProxyArray.fromList(children));
        } else {
            return this.react.exports().invokeMember("createElement", component, props);
        }
    }

    @NotNull Value reactCreateFragment(@Nullable ProxyObject props, @Nullable List<Object> children) {
        return reactCreateElement(fragmentSymbol, props, children);
    }

    @ScriptSafe
    public @NotNull Value wrapView(@NotNull Value component, @NotNull String inventoryType) {
        component.putMember("$$hcView", viewSymbol);
        component.putMember("inventoryType", inventoryType);
        return component;
    }

    /**
     * Gets the Minestom {@link InventoryType} from the given view. May be called on a view or a react element that is a view.
     *
     * @param view A view function or react element where the type is a view
     * @return The inventory type of the view
     * @throws IllegalArgumentException If the view is not a valid view
     */
    @NotNull InventoryType getInventoryType(@NotNull Value view) {
        // If this is a react element, we need to get the inner type
        if (view.getMember("$$typeof").equals(elementSymbol))
            view = view.getMember("type");

        // Ensure this looks like a view
        if (!view.getMember("$$hcView").equals(viewSymbol))
            throw new IllegalArgumentException("Views should be created with `view` from `@mapmaker/gui`");

        // Now get the inventory type
        final Value inventoryType = view.getMember("inventoryType");
        if (!inventoryType.isString())
            throw new IllegalArgumentException("Illegal view");
        return switch (inventoryType.asString()) {
            case "chest_6_row" -> InventoryType.CHEST_6_ROW;
            case "chest_3_row" -> InventoryType.CHEST_3_ROW;
            case "anvil" -> InventoryType.ANVIL;
            case "cartography" -> InventoryType.CARTOGRAPHY;
            default -> throw new IllegalArgumentException("Unknown inventory type");
        };
    }
}
