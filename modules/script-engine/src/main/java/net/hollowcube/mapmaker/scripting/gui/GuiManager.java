package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.cjs.Module;
import net.hollowcube.mapmaker.scripting.gui.react.JSX;
import net.hollowcube.mapmaker.scripting.gui.react.ReconcilerHostConfig;
import net.minestom.server.entity.Player;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class GuiManager {
    private final ScriptEngine engine;
    private final Module react;
    private final Value reactReconcilerInst;
    private final Map<String, Object> globals;

    public GuiManager(@NotNull ScriptEngine engine) {
        this.engine = engine;
        this.react = engine.load(URI.create("internal:///third_party/react/react.js"));
        this.reactReconcilerInst = engine.load(URI.create("internal:///third_party/react/react-reconciler.js"))
                .exports().execute(new ReconcilerHostConfig());

        this.globals = Map.of("JSX", new JSX(this.react));
    }

    /**
     * Opens the GUI at the given module. The module must have a default export of a React component with no props.
     *
     * @param player The player to open the GUI for
     * @param modulePath The module to load
     */
    public void openGui(@NotNull Player player, @NotNull URI modulePath) {
        final Value componentModule = this.engine.load(modulePath, this.globals).exports();
        if (!componentModule.hasMember("default"))
            throw new IllegalArgumentException("Module must have a default export");
        final Value component = componentModule.getMember("default");
        if (!component.canExecute())
            throw new IllegalArgumentException("Default export must be a functional component");
        final Value reactElement = this.react.exports().invokeMember("createElement", component, null);

        // Mount and do initial render immediately.
        final InventoryHost host = new InventoryHost(this, player);
        host.reconcilerRoot = mount(host);
        render(host, reactElement);

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

    private void render(@NotNull InventoryHost host, @NotNull Value reactElement) {
        reactReconcilerInst.invokeMember("updateContainer",
                /* element */ reactElement,
                /* container */ Objects.requireNonNull(host.reconcilerRoot),
                /* parentComponent */ null,
                /* callback */ null);
    }

}
