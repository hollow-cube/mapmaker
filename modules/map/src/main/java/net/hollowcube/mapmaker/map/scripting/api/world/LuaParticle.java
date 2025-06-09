package net.hollowcube.mapmaker.map.scripting.api.world;

import net.hollowcube.common.util.StringUtil;
import net.hollowcube.luau.LuaState;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;

/// Particle is the type representing a Minestom Particle.
/// We also add a global read-only table of all the existing particles.
public final class LuaParticle {
    public static final String NAME = "Particle";

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Minestom Particles
        state.newMetaTable(NAME);
        state.pushCFunction(LuaParticle::luaToString, "__tostring");
        state.setField(-2, "__tostring");
        // TODO: add call to configure particle data
        state.pop(1);

        // Global table of all particles
        state.newTable();
        for (var particle : Particle.values()) {
            var friendlyName = StringUtil.snakeToPascal(
                    particle.key().value().replace("/", "_"));

            push(state, particle);
            state.setField(-2, friendlyName);
        }
        state.setReadOnly(-1, true);
        state.setGlobal("Particle");
    }

    public static void push(@NotNull LuaState state, @NotNull Particle particle) {
        state.newUserData(particle);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull Particle checkArg(@NotNull LuaState state, int index) {
        return (Particle) state.checkUserDataArg(index, NAME);
    }

    private static int luaToString(@NotNull LuaState state) {
        var particle = checkArg(state, 1);
        state.pushString(particle.name());
        return 1;
    }

}
