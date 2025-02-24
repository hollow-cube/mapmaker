package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.dev.react.ReactReconcilerConstants;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// https://github.com/facebook/react/blob/main/packages/react-reconciler/src/forks/ReactFiberConfig.custom.js
@SuppressWarnings("unused")
public class ReactHostConfig {
    // BASE

    @HostAccess.Export
    public final boolean isPrimaryRenderer = true;

    @HostAccess.Export
    public @Nullable Value createInstance() {
        System.out.println("createInstance");
        return null;
    }

    @HostAccess.Export
    public @Nullable Value createTextInstance() {
        System.out.println("createTextInstance");
        return null;
    }

    @HostAccess.Export
    public void appendInitialChild() {
        System.out.println("appendInitialChild");
    }

    @HostAccess.Export
    public void finalizeInitialChildren() {
        System.out.println("finalizeInitialChildren");
    }

    @HostAccess.Export
    public boolean prepareUpdate() {
        return true;
    }

    @HostAccess.Export
    public boolean shouldSetTextContent() {
        return true;
    }

    @HostAccess.Export
    public @Nullable Value getRootHostContext(@NotNull Value rootContainer) {
        System.out.println("getRootHostContext");
        return null;
    }

    @HostAccess.Export
    public void getChildHostContext() {
        System.out.println("getChildHostContext");
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
    public void resetAfterCommit(@NotNull Value containerInfo) {
        System.out.println("resetAfterCommit");
    }

    @HostAccess.Export
    public void preparePortalMount(@NotNull Value containerInfo) {
        System.out.println("preparePortalMount");
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
    public void maySuspendCommit() {
        System.out.println("maySuspendCommit");
    }


    // EVENTS

    @HostAccess.Export
    public int getCurrentEventPriority() {
        System.out.println("getCurrentEventPriority");
        return ReactReconcilerConstants.defaultEventPriority;
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
    public void detachDeletedInstance() {
        System.out.println("detachDeletedInstance");
    }


    // SCHEDULER

    @HostAccess.Export
    public void setCurrentUpdatePriority(int priority) {
        System.out.println("setCurrentUpdatePriority: " + priority);
    }

    @HostAccess.Export
    public int getCurrentUpdatePriority() {
        System.out.println("getCurrentUpdatePriority");
        return ReactReconcilerConstants.defaultEventPriority;
    }

    @HostAccess.Export
    public int resolveUpdatePriority() {
        System.out.println("resolveUpdatePriority");
        return ReactReconcilerConstants.defaultEventPriority;
    }


    // MICROTASKS

    @HostAccess.Export
    public final boolean supportsMicrotasks = false;


    // TEST SELECTORS

    @HostAccess.Export
    public final boolean supportsTestSelectors = false;


    // MUTATION

    @HostAccess.Export
    public final boolean supportsMutation = true;

    @HostAccess.Export
    public void appendChild() {
        System.out.println("appendChild");
    }

    @HostAccess.Export
    public void appendChildToContainer() {
        System.out.println("appendChildToContainer");
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
    public void removeChild() {
        System.out.println("removeChild");
    }

    @HostAccess.Export
    public void removeChildFromContainer() {
        System.out.println("removeChildFromContainer");
    }

    @HostAccess.Export
    public void resetTextContent() {
        System.out.println("resetTextContent");
    }

    @HostAccess.Export
    public void commitTextUpdate() {
        System.out.println("commitTextUpdate");
    }

    @HostAccess.Export
    public void commitMount() {
        System.out.println("commitMount");
    }

    @HostAccess.Export
    public void commitUpdate() {
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


    // PERSISTENCE

    @HostAccess.Export
    public final boolean supportsPersistence = false;


    // HYDRATION

    @HostAccess.Export
    public final boolean supportsHydration = false;


    // RESOURCES

    @HostAccess.Export
    public final boolean supportsResources = false;


    // SINGLETONS

    @HostAccess.Export
    public final boolean supportsSingletons = false;

}
