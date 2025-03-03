package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.Module;
import net.hollowcube.mapmaker.scripting.gui.GuiManager;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.minestom.server.entity.Player;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static net.hollowcube.mapmaker.scripting.util.Proxies.wrapException;

public final class AtMapmakerGuiModule {
    private final GuiManager guiManager;
    private final Module react;

    public AtMapmakerGuiModule(@NotNull GuiManager guiManager, @NotNull Module react) {
        this.guiManager = guiManager;
        this.react = react;
    }

    private @NotNull InventoryHost useInventoryHost() {
        final Value hostContext = react.exports().getMember("__hollowcube_hostContext");
        if (hostContext == null || hostContext.isNull()) {
            throw wrapException("useInventoryHost must be called within a React component");
        }

        final Value hostValue = react.exports().invokeMember("useContext", hostContext);
        if (!hostValue.isHostObject()) {
            throw wrapException("useInventoryHost must be called within a React component");
        }

        return hostValue.as(InventoryHost.class);
    }

    @HostAccess.Export
    public Value view(@NotNull Value component, @NotNull String inventoryType) {
        return this.guiManager.wrapView(component, inventoryType);
    }

    @HostAccess.Export
    public Value useState(Value... args) {
        return react.exports().getMember("useState").execute((Object[]) args);
    }

    @HostAccess.Export
    public ViewStack useViewStack() {
        return new ViewStack(useInventoryHost());
    }

    public record ViewStack(@NotNull InventoryHost host) {

        @HostAccess.Export
        public void close() {
            final Set<Player> viewers = new HashSet<>(host.handle().getViewers());
            viewers.forEach(Player::closeInventory);
        }
    }
}
