package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.api.LibBase.EventSource.EventPusher;
import net.hollowcube.mapmaker.scripting.util.LuaCallback;
import net.hollowcube.mapmaker.scripting.util.LuaCoroutine;
import net.hollowcube.mapmaker.scripting.util.LuaEventListener;
import net.minestom.server.event.Event;
import net.minestom.server.event.trait.EntityEvent;

@LuaLibrary(name = "@mapmaker")
public final class LibBase {

    public static <E extends EntityEvent> void pushEventSource(LuaState state, Class<E> event, EventPusher<E> pusher) {
        LibBase$luau.pushEventSource(state, new EventSource(event, pusher));
    }

    /// A stream of events you can subscribe to. Use `:listen` to react every time the event
    /// fires, `:once` to handle just the next one, or `:wait` to pause the current thread
    /// until the next event.
    ///
    /// ```luau
    /// local players = require("@mapmaker/players")
    /// players.on_join:listen(function(player)
    ///     player:send_message("welcome!")
    /// end)
    /// ```
    ///
    /// @luaGeneric A...
    @LuaExport
    public static final class EventSource {

        @FunctionalInterface
        public interface EventPusher<E extends Event> {
            int pushArgs(LuaState state, E event);
        }

        private final Class<? extends Event> eventClass;
        private final EventPusher<? extends Event> pusher;

        private EventSource(Class<? extends Event> eventClass, EventPusher<? extends Event> pusher) {
            this.eventClass = eventClass;
            this.pusher = pusher;
        }

        /// Calls `handler` every time this event fires.
        ///
        /// @luaParam handler (A...) -> () - the function to run on each event
        @LuaMethod
        public int listen(LuaState state) {
            state.checkType(1, LuaType.FUNCTION);
            var cb = LuaCallback.of(state, 1);
            new LuaEventListener(eventClass, pusher, cb, false).register(state);
            // TODO: return a handle to cancel
            return 0;
        }

        /// Calls `handler` the next time this event fires, then unsubscribes.
        ///
        /// @luaParam handler (A...) -> () - the function to run on the next event
        @LuaMethod
        public int once(LuaState state) {
            state.checkType(1, LuaType.FUNCTION);
            var cb = LuaCallback.of(state, 1);
            new LuaEventListener(eventClass, pusher, cb, true).register(state);
            // TODO: return a handle to cancel
            return 0;
        }

        /// Pauses the calling thread until the next time this event fires, then resumes
        /// with the event's arguments.
        ///
        /// ```luau
        /// local player = players.on_join:wait()
        /// print(player.name .. " joined")
        /// ```
        ///
        /// @luaReturn A...
        @LuaMethod
        public int wait(LuaState state) {
            // No handler: the calling thread itself is resumed with the event
            // args. It is pinned by the coroutine handle and yielded just below.
            var co = LuaCoroutine.of(state);
            new LuaEventListener(eventClass, pusher, co, true).register(state);
            return state.yield(0);
        }
    }

}
