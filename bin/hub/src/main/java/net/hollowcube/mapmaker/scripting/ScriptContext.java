package net.hollowcube.mapmaker.scripting;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.UnaryOperator;

/// ScriptContext exists to manage resources associated with a given thread (group)
///
/// For example, the world itself gets a ScriptContext as well as any player scripts.
/// Threads created within those contexts will inherit the same ScriptContext (possibly
/// via a Holder, for example on a task or coroutine thread).
public sealed interface ScriptContext extends ThreadData, TagHandler {

    static ScriptContext get(LuaState state) {
        final Object data = state.getThreadData();
        if (data instanceof ThreadData threadData)
            return threadData.scriptContext();
        throw new IllegalStateException("ScriptContext not set (was '" + data + "')");
    }

    non-sealed interface World extends ScriptContext {

        MapWorld world();

    }

    non-sealed interface Player extends ScriptContext {

        MapPlayer player();

    }

    void track(Disposable disposable);

    Scheduler scheduler();

    /// Kinda unsafe to use since it may actually be filtered more than is obvious.
    EventNode<Event> eventNode();

    TagHandler tagHandler();

    //region ThreadData impl

    @Override
    default ScriptContext scriptContext() {
        return this;
    }

    //endregion

    //region TagHandler impl

    @Override
    default <T> @UnknownNullability T getTag(Tag<T> tag) {
        return tagHandler().getTag(tag);
    }

    @Override
    default <T> void setTag(Tag<T> tag, @Nullable T value) {
        tagHandler().setTag(tag, value);
    }

    @Override
    default <T> @Nullable T getAndSetTag(Tag<T> tag, @Nullable T value) {
        return tagHandler().getAndSetTag(tag, value);
    }

    @Override
    default <T> void updateTag(Tag<T> tag, UnaryOperator<@UnknownNullability T> value) {
        tagHandler().updateTag(tag, value);
    }

    @Override
    default <T> @UnknownNullability T updateAndGetTag(Tag<T> tag, UnaryOperator<@UnknownNullability T> value) {
        return tagHandler().updateAndGetTag(tag, value);
    }

    @Override
    default <T> @UnknownNullability T getAndUpdateTag(Tag<T> tag, UnaryOperator<@UnknownNullability T> value) {
        return tagHandler().getAndUpdateTag(tag, value);
    }

    @Override
    default TagReadable readableCopy() {
        return tagHandler().readableCopy();
    }

    @Override
    default TagHandler copy() {
        return tagHandler().copy();
    }

    @Override
    default void updateContent(CompoundBinaryTag compound) {
        tagHandler().updateContent(compound);
    }

    @Override
    default CompoundBinaryTag asCompound() {
        return tagHandler().asCompound();
    }

    //endregion
}
