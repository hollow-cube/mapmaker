package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.Module;
import net.hollowcube.mapmaker.scripting.gui.GuiManager;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.minestom.server.entity.Player;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static net.hollowcube.mapmaker.scripting.util.Proxies.wrapException;

public final class AtMapmakerGuiModule implements ProxyObject {
    private static final Set<String> KNOWN_KEYS = Set.of("view", "useState", "use", "useViewStack");

    private final GuiManager guiManager;
    private final Module react;

    public AtMapmakerGuiModule(@NotNull GuiManager guiManager, @NotNull Module react) {
        this.guiManager = guiManager;
        this.react = react;
    }

    @HostAccess.Export
    public Value view(@NotNull Value component, @NotNull String inventoryType) {
        try {
            return this.guiManager.wrapView(component, inventoryType);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public Value useState(Value... args) {
        try {
            return react.exports().getMember("useState").execute((Object[]) args);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public Value use(Value... args) {
        try {
            return react.exports().invokeMember("use", (Object[]) args);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public ViewStack useViewStack() {
        return ViewStack.INSTANCE;
    }

    @Override
    public Object getMember(String key) {
        try {
            return switch (key) {
                case "view" -> (ProxyExecutable) (args) -> {
                    if (args.length != 2) {
                        throw new IllegalArgumentException("view() expects 2 arguments");
                    }

                    return view(args[0], args[1].asString());
                };
                case "useState" -> react.exports().getMember("useState");
                case "use" -> react.exports().getMember("use");
                case "useViewStack" -> (ProxyExecutable) (args) -> {
                    if (args.length != 0) {
                        throw new IllegalArgumentException("useViewStack() expects 0 arguments");
                    }

                    return useViewStack();
                };
                case null, default -> null;
            };
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @Override
    public Object getMemberKeys() {
        return KNOWN_KEYS;
    }

    @Override
    public boolean hasMember(String key) {
        return KNOWN_KEYS.contains(key);
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("putMember() not supported.");
    }

    public static final class ViewStack {
        public static final ViewStack INSTANCE = new ViewStack();

        @HostAccess.Export
        public void pushView(@NotNull Value element, Value... restTodo) {
            try {
                InventoryHost.current().pushView(element);
            } catch (Exception e) {
                throw wrapException(e);
            }
        }

        @HostAccess.Export
        public void popView() {
            try {
                InventoryHost.current().popView();
            } catch (Exception e) {
                throw wrapException(e);
            }
        }

        @HostAccess.Export
        public void close() {
            try {
                final Set<Player> viewers = new HashSet<>(InventoryHost.current().handle().getViewers());
                viewers.forEach(Player::closeInventory);
            } catch (Exception e) {
                throw wrapException(e);
            }
        }

        private ViewStack() {
        }
    }
}
