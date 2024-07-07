package net.hollowcube.mapmaker.map.script.object2;

// export type RBXScriptSignal<T... = ...any> = {
//    Wait: (self: RBXScriptSignal<T...>) -> T...,
//    Connect: (self: RBXScriptSignal<T...>, callback: (T...) -> ()) -> RBXScriptConnection,
//    ConnectParallel: (self: RBXScriptSignal<T...>, callback: (T...) -> ()) -> RBXScriptConnection,
//    Once: (self: RBXScriptSignal<T...>, callback: (T...) -> ()) -> RBXScriptConnection,
//}

import net.hollowcube.luau.annotation.LuaMethod;

public interface LuaEventSource<F> {

    @LuaMethod
    void listen(F callback);

}
