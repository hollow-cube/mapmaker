package net.hollowcube.mapmaker.scripting.gui.react;

import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import net.hollowcube.mapmaker.scripting.gui.node.*;
import net.hollowcube.mapmaker.scripting.util.Proxies;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Map;
import java.util.Objects;

/**
 * Implements the React Reconciler Host Config API.
 */
@SuppressWarnings("unused")
public class ReconcilerHostConfig {
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
                default -> throw new IllegalArgumentException("Unknown element type: " + type);
            };
            node.updateFromProps(props);
            return node;
        } catch (Exception e) {
            throw jsError(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @HostAccess.Export
    public @NotNull Node createTextInstance(@UnknownNullability String text, @NotNull Value rootContainer, @NotNull Value hostContext, @NotNull Value internalHandle) {
        return new TextNode.Raw(Objects.requireNonNull(text, ""));
    }

    @HostAccess.Export
    public void appendInitialChild(@NotNull Node parent, @NotNull Node child) {
        appendChild(parent, child);
    }

    @HostAccess.Export
    public boolean finalizeInitialChildren(@NotNull Value instance, @NotNull String type, @NotNull Value props, @NotNull Value hostContext) {
        // TODO: This returns true if work needs to be performed when 'finalizing', ie connecting to the 'screen'.
        // if true is returned then this node will get a commitMount later.
        // In our case we may want to defer event handlers or something.

        System.out.println("finalizeInitialChildren");
        return false;
    }

    @HostAccess.Export
    public boolean prepareUpdate() {
        System.out.println("prepareUpdate");
        return true;
    }

    @HostAccess.Export
    public boolean shouldSetTextContent(@NotNull String type, @NotNull Value props) {
        return false;
    }

    @HostAccess.Export
    public @NotNull ProxyObject getRootHostContext(@NotNull Value rootContainer) {
        return Proxies.freezeObject(Proxies.proxyObject(Map.of())); // TODO
    }

    @HostAccess.Export
    public @NotNull Value getChildHostContext(@NotNull Value parentHostContext, @NotNull String type) {
        return parentHostContext;
    }

    @HostAccess.Export
    public @NotNull Value getPublicInstance(@NotNull Value instance) {
        System.out.println("getPublicInstance");
        return instance;
    }

    @HostAccess.Export
    public @Nullable Value prepareForCommit(@NotNull Value containerInfo) {
        System.out.println("prepareForCommit");
        return null;
    }

    @HostAccess.Export
    public void resetAfterCommit(@NotNull InventoryHost container) {
        // We have done an update
        System.out.println("resetAfterCommit");
        container.queueRedraw();
    }

    @HostAccess.Export
    public void preparePortalMount(@NotNull Value containerInfo) {
    }

    @HostAccess.Export
    public void scheduleTimeout() {
        System.out.println("scheduleTimeout");
    }

    @HostAccess.Export
    public void cancelTimeout() {
        System.out.println("cancelTimeout");
    }

    @HostAccess.Export
    public int noTimeout = -1;

    @HostAccess.Export
    public boolean maySuspendCommit(@NotNull String type, @NotNull Value props) {
        System.out.println("maySuspendCommit");
        return false;
    }


    // EVENTS

    @HostAccess.Export
    public int getCurrentEventPriority() {
        System.out.println("getCurrentEventPriority");
        return ReconcilerConstants.defaultEventPriority;
    }

    @HostAccess.Export
    public @Nullable Value getInstanceFromNode() {
        System.out.println("getInstanceFromNode");
        return null;
    }

    @HostAccess.Export
    public void beforeActiveInstanceBlur() {
        System.out.println("beforeActiveInstanceBlur");
    }

    @HostAccess.Export
    public void afterActiveInstanceBlur() {
        System.out.println("afterActiveInstanceBlur");
    }

    @HostAccess.Export
    public void prepareScopeUpdate() {
        System.out.println("prepareScopeUpdate");
    }

    @HostAccess.Export
    public @Nullable Value getInstanceFromScope() {
        System.out.println("getInstanceFromScope");
        return null;
    }

    @HostAccess.Export
    public void detachDeletedInstance(@NotNull Node instance) {
        // noop
    }


    // SCHEDULER

    @HostAccess.Export
    public void setCurrentUpdatePriority(int priority) {
        System.out.println("setCurrentUpdatePriority: " + priority);
    }

    @HostAccess.Export
    public int getCurrentUpdatePriority() {
        System.out.println("getCurrentUpdatePriority");
        return ReconcilerConstants.defaultEventPriority;
    }

    @HostAccess.Export
    public int resolveUpdatePriority() {
        System.out.println("resolveUpdatePriority");
        return ReconcilerConstants.defaultEventPriority;
    }


    // MUTATION

    @HostAccess.Export
    public final boolean supportsMutation = true;

    @HostAccess.Export
    public void appendChild(@NotNull Node parent, @NotNull Node child) {
        if (!(parent instanceof GroupNode group)) {
            throw new IllegalArgumentException(parent.type() + " may not have children");
        }
        group.appendChild(child);
    }

    @HostAccess.Export
    public void appendChildToContainer(@NotNull InventoryHost parent, @NotNull Node child) {
        parent.addChild(child);
    }

    @HostAccess.Export
    public void insertBefore() {
        System.out.println("insertBefore");
    }

    @HostAccess.Export
    public void insertInContainerBefore() {
        System.out.println("insertInContainerBefore");
    }

    @HostAccess.Export
    public void removeChild(@NotNull Node parent, @NotNull Node child, @NotNull Value unknown1, @NotNull Value unknown2, @NotNull Value unknown3) {
        if (!(parent instanceof GroupNode group)) {
            throw new IllegalArgumentException(parent.type() + " may not have children");
        }
        group.removeChild(child);
    }

    @HostAccess.Export
    public void removeChildFromContainer(@NotNull Value container, @NotNull Value child, @NotNull Value unknown1, @NotNull Value unknown2, @NotNull Value unknown3) {
        System.out.println("removeChildFromContainer");
    }

    @HostAccess.Export
    public void resetTextContent() {
        System.out.println("resetTextContent");
    }

    @HostAccess.Export
    public void commitTextUpdate(@NotNull TextNode.Raw textInstance, @NotNull String oldText, @NotNull String newText, @NotNull Value unknown1, @NotNull Value unknown2) {
        textInstance.setContent(newText);
        System.out.println("commitTextUpdate");
    }

    @HostAccess.Export
    public void commitMount() {
        System.out.println("commitMount");
    }

    @HostAccess.Export
    public void commitUpdate(@NotNull Node instance, @NotNull String type, @NotNull Value prevProps, @NotNull Value nextProps, @NotNull Value internalHandle) {
        if (!instance.type().equals(type))
            throw new UnsupportedOperationException("instance type changed, " + instance.type() + " != " + type);

        instance.updateFromProps(nextProps);
        System.out.println("commitUpdate");
    }

    @HostAccess.Export
    public void hideInstance() {
        System.out.println("hideInstance");
    }

    @HostAccess.Export
    public void hideTextInstance() {
        System.out.println("hideTextInstance");
    }

    @HostAccess.Export
    public void unhideInstance() {
        System.out.println("unhideInstance");
    }

    @HostAccess.Export
    public void unhideTextInstance() {
        System.out.println("unhideTextInstance");
    }

    @HostAccess.Export
    public void clearContainer(@NotNull Value container) {
        System.out.println("clearContainer");
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


    private static @NotNull RuntimeException jsError(@NotNull String message) {
        // TODO: we should cache the error constructor in the engine probably
        Value jsBindings = Context.getCurrent().getBindings("js");
        Value errorConstructor = jsBindings.getMember("Error");
        Value errorObject = errorConstructor.newInstance(message);
        return errorObject.throwException();
    }
}
