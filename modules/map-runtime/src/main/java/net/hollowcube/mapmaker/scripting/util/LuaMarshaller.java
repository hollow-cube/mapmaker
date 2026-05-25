package net.hollowcube.mapmaker.scripting.util;

import net.hollowcube.luau.LuaState;
import org.jetbrains.annotations.UnknownNullability;

@FunctionalInterface
public interface LuaMarshaller<T extends @UnknownNullability Object> {

    int marshal(LuaState state, T value);

}
