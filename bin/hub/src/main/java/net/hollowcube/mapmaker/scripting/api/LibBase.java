package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.entity.EntityAttackEvent;

@LuaLibrary(name = "@mapmaker")
public final class LibBase {

    @LuaExport
    public static final class EventSource {

        @LuaMethod
        public int listen(LuaState state) {
            state.checkType(1, LuaType.FUNCTION);
            int handlerRef = state.ref(1);

            MinecraftServer.getGlobalEventHandler()
                .addListener(EntityAttackEvent.class, event -> {
//                    var attacker = event.getEntity();

                    state.getRef(handlerRef);
                    state.call(0, 0);
                });

            return 0;
        }

    }

}
