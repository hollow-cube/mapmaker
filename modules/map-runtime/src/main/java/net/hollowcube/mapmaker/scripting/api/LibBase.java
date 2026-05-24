package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.util.LuaCallback;
import net.hollowcube.mapmaker.scripting.util.LuaMarshaller;
import net.hollowcube.mapmaker.scripting.util.LuaResumable;
import net.hollowcube.scripting.gen.LuaExport;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.gen.LuaMethod;
import net.hollowcube.scripting.gen.LuaProperty;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.Nullable;

@LuaLibrary(name = "@mapmaker")
public final class LibBase {

    public static <E extends EntityEvent> void pushSignal(LuaState state, Class<E> event, LuaMarshaller<E> pusher) {
        LibBase$luau.pushSignal(state, new Signal(event, pusher));
    }

    /// A stream of events you can subscribe to. Use `:connect` to react every time the event
    /// fires, `:once` to handle just the next one, or `:wait` to pause the current thread
    /// until the next event.
    ///
    /// ```luau
    /// local players = require("@mapmaker/players")
    /// players.on_join:connect(function(player)
    ///     player:send_message("welcome!")
    /// end)
    /// ```
    ///
    /// @luaGeneric A...
    @LuaExport
    public static final class Signal {
        private final Class<? extends Event> eventClass;
        private final LuaMarshaller<? extends Event> marshaller;

        private Signal(Class<? extends Event> eventClass, LuaMarshaller<? extends Event> marshaller) {
            this.eventClass = eventClass;
            this.marshaller = marshaller;
        }

        /// Calls `handler` every time this event fires.
        ///
        /// @luaParam handler (A...) -> () - the function to run on each event
        /// @luaReturn Connection - a reference to the newly created connection
        @LuaMethod
        public int connect(LuaState state) {
            var callback = LuaCallback.of(state, 1);

            var handle = new Connection(eventClass, marshaller, callback);
            handle.registerAndTrack(state);
            return handle.push(state);
        }

        /// Calls `handler` the next time this event fires, then unsubscribes.
        ///
        /// @luaParam handler (A...) -> () - the function to run on the next event
        /// @luaReturn Connection - a reference to the newly created connection
        @LuaMethod
        public int once(LuaState state) {
            var callback = LuaCallback.of(state, 1);

            var handle = new Connection(eventClass, marshaller, callback);
            handle.singleShot = true;
            handle.registerAndTrack(state);
            return handle.push(state);
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
            throw new UnsupportedOperationException("not implemented"); // todo
//            var handle = new Connection(eventClass, marshaller, null);
//            handle.singleShot = true;
//            handle.registerAndTrack(state);
//            return state.yield(0);
        }
    }

    @LuaExport
    public static final class Connection implements EventListener<Event>, Disposable {
        private final Class<Event> eventType;
        private final LuaMarshaller<Event> marshaller;
        private final LuaResumable handler;
        boolean singleShot = false;

        private @Nullable EventNode<InstanceEvent> node;
        private boolean disposed;

        @SuppressWarnings("unchecked")
        private Connection(
            Class<? extends Event> eventType,
            LuaMarshaller<? extends Event> marshaller,
            LuaResumable handler
        ) {
            this.eventType = (Class<Event>) eventType;
            this.marshaller = (LuaMarshaller<Event>) marshaller;
            this.handler = handler;
        }

        /// Returns true if this connection is currently active, false otherwise.
        ///
        /// A connection becomes inactive if it is explicitly disconnected, if it was
        /// a single-use connection that has already fired.
        ///
        /// @luaReturn boolean
        @LuaProperty
        public int getDisconnected(LuaState state) {
            state.pushBoolean(disposed);
            return 1;
        }

        /// Disconnects this handler from the event.
        /// After calling this, the handler will no longer be called when the event fires.
        @LuaMethod
        public void disconnect(LuaState state) {
            dispose();
        }

        //region Event and disposable impl

        @Override
        public Class<Event> eventType() {
            return eventType;
        }

        @Override
        public Result run(Event event) {
            if (disposed || handler.isDisposed())
                return Result.EXPIRED;

            int nargs = marshaller.marshal(handler.state(), event);
            if (nargs < 0) return Result.INVALID; // marshaller ignored this event.

            handler.resume(nargs);

            if (singleShot) {
                dispose();
                return Result.EXPIRED;
            }
            return Result.SUCCESS;
        }

        @Override
        public void dispose() {
            if (disposed) return;
            if (node != null) {
                //noinspection rawtypes,unchecked
                node.removeListener((EventListener) this);
            }
            disposed = true;
            handler.dispose();
        }

        private void registerAndTrack(LuaState state) {
            var context = ScriptContext.current(state);
            if (context == null) {
                handler.dispose();
                return;
            }

            this.node = context.runtime().world().eventNode();
            //noinspection rawtypes,unchecked
            node.addListener((EventListener) this);
            context.track(this);
        }

        private int push(LuaState state) {
            LibBase$luau.pushConnection(state, this);
            return 1;
        }

        //endregion
    }

}
