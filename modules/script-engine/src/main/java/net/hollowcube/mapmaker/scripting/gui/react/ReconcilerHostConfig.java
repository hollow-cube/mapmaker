package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.hollowcube.mapmaker.scripting.gui.node.*;
import net.hollowcube.mapmaker.scripting.util.Proxies;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Map;
import java.util.Objects;

import static net.hollowcube.mapmaker.scripting.util.Proxies.wrapException;

/**
 * Implements the React Reconciler Host Config API.
 */
@SuppressWarnings("unused")
public class ReconcilerHostConfig {
    private final ScriptEngine engine;

    public ReconcilerHostConfig(@NotNull ScriptEngine engine) {
        this.engine = engine;
    }

    // BASE

    @HostAccess.Export
    public final boolean isPrimaryRenderer = true;

    @HostAccess.Export
    public @NotNull Node createInstance(@NotNull String type, @NotNull Value props, @NotNull Value rootContainer, @NotNull Value hostContext, @NotNull Value ignoredInternalHandle) {
        try {
            final Node node = switch (type) {
                case "group" -> new GroupNode();
                case "text" -> new TextNode();
                case "button" -> new ButtonNode();
                case "tooltip" -> new TooltipNode();
                case "sprite" -> new SpriteNode();
                case "item" -> new ItemNode();
                case "gap" -> new GapNode();
                default -> throw new IllegalArgumentException("Unknown element type: " + type);
            };
            node.updateFromProps(props);
            return node;
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public @NotNull Node createTextInstance(@UnknownNullability String text, @NotNull Value rootContainer, @NotNull Value hostContext, @NotNull Value internalHandle) {
        try {
            return new TextNode.Raw(Objects.requireNonNull(text, ""));
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void appendInitialChild(@NotNull Node parent, @NotNull Node child) {
        try {
            appendChild(parent, child);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public boolean finalizeInitialChildren(@NotNull Value instance, @NotNull String type, @NotNull Value props, @NotNull Value hostContext) {
        return false; // No extra work is ever needed (commitMount also not implemented)
    }

    @HostAccess.Export
    public boolean shouldSetTextContent(@NotNull String type, @NotNull Value props) {
        // Returning true would mean that any node can have its content set to text, ie the html equivalent of
        // node.textContent = 'somestring'
        // We don't support that so always return false. If returning true we would need to implement resetTextContent.
        return false;
    }

    @HostAccess.Export
    public @NotNull ProxyObject getRootHostContext(@NotNull InventoryHost rootContainer) {
        return Proxies.freezeObject(Proxies.proxyObject(Map.of())); // TODO
    }

    @HostAccess.Export
    public @NotNull Value getChildHostContext(@NotNull Value parentHostContext, @NotNull String type) {
        return parentHostContext;
    }

    @HostAccess.Export
    public @NotNull Value getPublicInstance(@NotNull Value instance) {
        return instance;
    }

    @HostAccess.Export
    public @Nullable Value prepareForCommit(@NotNull Value containerInfo) {
        return null; // Noop
    }

    @HostAccess.Export
    public void resetAfterCommit(@NotNull InventoryHost container) {
        try {
            container.queueRedraw(); // We have done an update, redraw at end of tick
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void preparePortalMount(@NotNull Value containerInfo) {
        // noop
    }

    @HostAccess.Export
    public Object scheduleTimeout(@NotNull Value... arguments) {
        return engine.globals.setTimeout(arguments);
    }

    @HostAccess.Export
    public void cancelTimeout(@Nullable Integer taskId) {
        if (taskId == null) return; // Skull emoji
        engine.globals.clearTimeout(Value.asValue(taskId));
    }

    @HostAccess.Export
    public int noTimeout = -1;

    @HostAccess.Export
    public boolean maySuspendCommit(@NotNull String type, @NotNull Value props) {
        return false; // Noop
    }

    @HostAccess.Export
    public void startSuspendingCommit() {
        // noop
    }

    @HostAccess.Export
    public Value waitForCommitToBeReady() {
        return null; // Commit immediately
    }

    @HostAccess.Export
    public @Nullable Value getInstanceFromNode() {
        return null; // Noop
    }

    @HostAccess.Export
    public void beforeActiveInstanceBlur() {
        // noop
    }

    @HostAccess.Export
    public void afterActiveInstanceBlur() {
        // noop
    }

    @HostAccess.Export
    public void detachDeletedInstance(@NotNull Node instance) {
        // noop
    }


    // SCHEDULER

    private int currentUpdatePriority = ReconcilerConstants.noEventPriority;

    @HostAccess.Export
    public void setCurrentUpdatePriority(int priority) {
        this.currentUpdatePriority = priority;
    }

    @HostAccess.Export
    public int getCurrentUpdatePriority() {
        return currentUpdatePriority;
    }

    @HostAccess.Export
    public int resolveUpdatePriority() {
        if (currentUpdatePriority == ReconcilerConstants.noEventPriority)
            return ReconcilerConstants.defaultEventPriority;
        return currentUpdatePriority;
    }


    // MUTATION

    @HostAccess.Export
    public final boolean supportsMutation = true;

    @HostAccess.Export
    public void appendChild(@NotNull Node parent, @NotNull Node child) {
        try {
            if (!(parent instanceof GroupNode group)) {
                throw new IllegalArgumentException(parent.type() + " may not have children");
            }
            group.appendChild(child);

        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void appendChildToContainer(@NotNull InventoryHost parent, @NotNull Node child) {
        try {
            parent.addChild(child);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void insertBefore(@NotNull Node parent, @NotNull Node child, @NotNull Node beforeChild) {
        try {
            if (!(parent instanceof GroupNode group)) {
                throw new IllegalArgumentException(parent.type() + " may not have children");
            }
            group.insertBefore(child, beforeChild);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void insertInContainerBefore(@NotNull InventoryHost parent, @NotNull Node child, @NotNull Node beforeChild) {
        throw wrapException("container may only have one child");
    }

    @HostAccess.Export
    public void removeChild(@NotNull Node parent, @NotNull Node child, @NotNull Value unknown1, @NotNull Value unknown2, @NotNull Value unknown3) {
        try {
            if (!(parent instanceof GroupNode group)) {
                throw new IllegalArgumentException(parent.type() + " may not have children");
            }
            group.removeChild(child);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void removeChildFromContainer(@NotNull InventoryHost container, @NotNull Node child, @NotNull Value unknown1, @NotNull Value unknown2, @NotNull Value unknown3) {
        container.removeChild(child);
    }

    @HostAccess.Export
    public void commitTextUpdate(@NotNull TextNode.Raw textInstance, @NotNull String oldText, @NotNull String newText, @NotNull Value unknown1, @NotNull Value unknown2) {
        try {
            textInstance.setContent(newText);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void clearContainer(@NotNull InventoryHost container) {
        container.clear();
    }

    @HostAccess.Export
    public void commitUpdate(@NotNull Node instance, @NotNull String type, @NotNull Value prevProps, @NotNull Value nextProps, @NotNull Value internalHandle) {
        try {
            if (!instance.type().equals(type))
                throw new UnsupportedOperationException("instance type changed, " + instance.type() + " != " + type);

            instance.updateFromProps(nextProps);
        } catch (Exception e) {
            throw wrapException(e);
        }
    }

    @HostAccess.Export
    public void hideInstance(@NotNull Node instance, @NotNull Value props, @NotNull Value unknown1, @NotNull Value unknown2, @NotNull Value unknown3) {
        instance.setHidden(true);
    }

    @HostAccess.Export
    public void hideTextInstance() {
        // Noop
    }

    @HostAccess.Export
    public void unhideInstance(@NotNull Node instance, @NotNull Value props, @NotNull Value unknown1, @NotNull Value unknown2, @NotNull Value unknown3) {
        instance.setHidden(false);
    }

    @HostAccess.Export
    public void unhideTextInstance() {
        // Noop
    }


    // VARIOUS UNSUPPORTED FEATURES

    @HostAccess.Export
    public final boolean supportsMicrotasks = false;

    @HostAccess.Export
    public final boolean supportsTestSelectors = false;

    @HostAccess.Export
    public final boolean supportsPersistence = false;

    @HostAccess.Export
    public final boolean supportsHydration = false;

    @HostAccess.Export
    public final boolean supportsResources = false;

    @HostAccess.Export
    public final boolean supportsSingletons = false;

}
