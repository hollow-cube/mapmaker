package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.cjs.Module;
import net.hollowcube.mapmaker.scripting.gui.react.AtMapmakerGuiModule;
import net.hollowcube.mapmaker.scripting.gui.react.JSX;
import net.hollowcube.mapmaker.scripting.gui.react.ReconcilerHostConfig;
import net.hollowcube.mapmaker.scripting.util.Garbage;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static net.hollowcube.mapmaker.scripting.util.Proxies.proxyObject;

public class GuiManager {
    private final ScriptEngine engine;
    final Module react; //todo make me private again
    private final Value reactReconcilerInst;
    private final Value hostContext;

    private final Map<String, Object> globals;
    private final Map<String, Object> extraModules;

    public GuiManager(@NotNull ScriptEngine engine) {
        this.engine = engine;

        // todo yikes, find out why we suddenly need this
        engine.context().getBindings("js").putMember("window", engine.context().getBindings("js"));

        // todo should only run in a 'development' environment
        setupReactRefresh();

        this.react = engine.load(URI.create("internal:///third_party/react/react.js"));
        this.reactReconcilerInst = engine.load(URI.create("internal:///third_party/react/react-reconciler.js"))
                .exports().execute(new ReconcilerHostConfig());
        this.reactReconcilerInst.invokeMember("injectIntoDevTools");

        this.hostContext = this.react.exports().invokeMember("createContext");
        this.react.exports().putMember("__hollowcube_hostContext", this.hostContext);

        this.globals = Map.of("JSX", new JSX(this.react));
        this.extraModules = Map.of("@mapmaker/gui", new AtMapmakerGuiModule(this.react));
    }

    /**
     * Opens the GUI at the given module. The module must have a default export of a React component with no props.
     *
     * @param player The player to open the GUI for
     * @param modulePath The module to load
     */
    public void openGui(@NotNull Player player, @NotNull URI modulePath) {
        // TODO: dont wildly duplicate this code for react refresh with Module
        var globals = new HashMap<>(this.globals);
        globals.put("__hollowcube_moduleId", modulePath.toString());
        final Value componentModule = this.engine.load(modulePath, globals, this.extraModules, (code) -> {
            return String.format(Garbage.REACT_REFRESH_MODULE_TEMPLATE, code);
        }).exports();
        if (!componentModule.hasMember("default"))
            throw new IllegalArgumentException("Module must have a default export");
        final Value component = componentModule.getMember("default");
        if (!component.canExecute())
            throw new IllegalArgumentException("Default export must be a functional component");
        final Value reactElement = this.react.exports().invokeMember("createElement", component, null);

        final InventoryHost host = new InventoryHost(this, player);

        // Wrap the element in a context provider
        final Value contextProviderElement = this.react.exports().invokeMember("createElement",
                this.hostContext.getMember("Provider"),
                proxyObject(Map.of("value", host)),
                reactElement);

        // Mount and do initial render immediately.
        host.reconcilerRoot = mount(host);
        render(host, contextProviderElement);

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
        System.out.println("UNMOUNTING");
        reactReconcilerInst.invokeMember("updateContainer",
                /* element */ null,
                /* container */ Objects.requireNonNull(host.reconcilerRoot),
                /* parentComponent */ null,
                /* callback */ null);
    }

    public void render(@NotNull InventoryHost host, @NotNull Value reactElement) {
        reactReconcilerInst.invokeMember("updateContainer",
                /* element */ reactElement,
                /* container */ Objects.requireNonNull(host.reconcilerRoot),
                /* parentComponent */ null,
                /* callback */ null);

        // 'Render' the underlying Minestom inventory (items & title)
        host.drawCurrentElement(InventoryType.CHEST_6_ROW); // TODO read `inventoryType` from module.
    }

    private void setupReactRefresh() {
        // Load react-refresh runtime _before_ any other react imports. It must be done before for it to work.
        var reactRefreshRuntime = engine.load(URI.create("internal:///third_party/react/react-refresh-runtime.js"));
        var globalThis = engine.context().getBindings("js");

        // Notably this injects into the global namespace which means we leak react details to other scripts. I think this is bad and we
        // instead should handle this with the Module implementation (ie call this with a new object and expose all the properties added
        // to child modules so it never ends up in the context global scope).
        // TODO: see above
        reactRefreshRuntime.exports().invokeMember("injectIntoGlobalHook", globalThis);
        globalThis.putMember("$RefreshReg$", engine.context().eval("js", "() => {}"));
        globalThis.putMember("$RefreshSig$", engine.context().eval("js", "() => type => type"));
        globalThis.putMember("enqueueUpdate", (ProxyExecutable) (args) -> {
            reactRefreshRuntime.exports().invokeMember("performReactRefresh");
            return null;
        });
    }
}
