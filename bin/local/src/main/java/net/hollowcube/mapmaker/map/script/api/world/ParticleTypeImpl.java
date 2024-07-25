package net.hollowcube.mapmaker.map.script.api.world;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMeta;
import net.hollowcube.luau.annotation.LuaTypeImpl;
import net.hollowcube.mapmaker.map.script.api.item.ItemStackTypeImpl;
import net.hollowcube.mapmaker.map.script.api.math.VectorTypeImpl;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Point;
import net.minestom.server.particle.Particle;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@LuaTypeImpl(type = Particle.class, name = "ParticleType")
public class ParticleTypeImpl {

    public static void init(@NotNull LuaState state) {
        ParticleTypeImpl$Wrapper.initMetatable(state);

        // Build the particle table
        state.newTable();
        for (var particle : Particle.values()) {
            state.newUserData(particle);
            state.getMetaTable(ParticleTypeImpl$Wrapper.TYPE_NAME);
            state.setMetaTable(-2);
            state.setField(-2, particle.namespace().path().toUpperCase(Locale.ROOT));
        }
        state.setReadOnly(-1, true);
        state.setGlobal("particles");
    }

    public static void pushLuaValue(@NotNull LuaState state, @NotNull Particle particle) {
        state.newUserData(particle);
        state.getMetaTable(ParticleTypeImpl$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull Particle checkLuaArg(@NotNull LuaState state, int index) {
        return (Particle) state.checkUserDataArg(index, ParticleTypeImpl$Wrapper.TYPE_NAME);
    }

    @LuaMeta(LuaMeta.Type.CALL)
    public static int luaCall(@NotNull LuaState state) {
        var particle = checkLuaArg(state, 1);
        switch (particle) {
            case Particle.Block b -> setBlockParticleData(state, b);
            case Particle.BlockMarker b -> setBlockMarkerParticleData(state, b);
            case Particle.Dust d -> setDustParticleData(state, d);
            case Particle.DustColorTransition d -> setDustColorTransitionParticleData(state, d);
            case Particle.DustPillar d -> setDustPillarParticleData(state, d);
            case Particle.EntityEffect e -> setEntityEffectParticleData(state, e);
            case Particle.FallingDust f -> setFallingDustParticleData(state, f);
            case Particle.Item i -> setItemParticleData(state, i);
            case Particle.SculkCharge s -> setSculkChargeParticleData(state, s);
            case Particle.Shriek s -> setShriekParticleData(state, s);
            case Particle.Vibration v -> setVibrationParticleData(state, v);
            default -> state.error("Particle type is not callable");
        }
        return 1;
    }

    // Particle data implementations

    private static void setBlockParticleData(@NotNull LuaState state, @NotNull Particle.Block particle) {
        pushLuaValue(state, particle.withBlock(BlockTypeImpl.checkLuaArg(state, 2)));
    }

    private static void setBlockMarkerParticleData(@NotNull LuaState state, @NotNull Particle.BlockMarker particle) {
        pushLuaValue(state, particle.withBlock(BlockTypeImpl.checkLuaArg(state, 2)));
    }

    private static void setDustParticleData(@NotNull LuaState state, @NotNull Particle.Dust particle) {
        Point color = VectorTypeImpl.checkLuaArg(state, 2);
        float size = (float) state.checkNumberArg(3);
        pushLuaValue(state, particle.withProperties(new Color(color.blockX(), color.blockY(), color.blockZ()), size));
    }

    private static void setDustColorTransitionParticleData(@NotNull LuaState state, @NotNull Particle.DustColorTransition particle) {
        Point color = VectorTypeImpl.checkLuaArg(state, 2);
        Point transitionColor = VectorTypeImpl.checkLuaArg(state, 3);
        float size = (float) state.checkNumberArg(4);
        pushLuaValue(state, particle.withProperties(new Color(color.blockX(), color.blockY(), color.blockZ()),
                new Color(transitionColor.blockX(), transitionColor.blockY(), transitionColor.blockZ()), size));
    }

    private static void setDustPillarParticleData(@NotNull LuaState state, @NotNull Particle.DustPillar particle) {
        pushLuaValue(state, particle.withBlock(BlockTypeImpl.checkLuaArg(state, 2)));
    }

    private static void setEntityEffectParticleData(@NotNull LuaState state, @NotNull Particle.EntityEffect particle) {
        Point color = VectorTypeImpl.checkLuaArg(state, 2);
        int alpha = state.checkIntegerArg(3);
        pushLuaValue(state, particle.withColor(alpha, new Color(color.blockX(), color.blockY(), color.blockZ())));
    }

    private static void setFallingDustParticleData(@NotNull LuaState state, @NotNull Particle.FallingDust particle) {
        pushLuaValue(state, particle.withBlock(BlockTypeImpl.checkLuaArg(state, 2)));
    }

    private static void setItemParticleData(@NotNull LuaState state, @NotNull Particle.Item particle) {
        pushLuaValue(state, particle.withItem(ItemStackTypeImpl.checkLuaArg(state, 2)));
    }

    private static void setSculkChargeParticleData(@NotNull LuaState state, @NotNull Particle.SculkCharge particle) {
        pushLuaValue(state, particle.withRoll((float) state.checkNumberArg(2)));
    }

    private static void setShriekParticleData(@NotNull LuaState state, @NotNull Particle.Shriek particle) {
        pushLuaValue(state, particle.withDelay(state.checkIntegerArg(2)));
    }

    private static void setVibrationParticleData(@NotNull LuaState state, @NotNull Particle.Vibration particle) {
        throw new UnsupportedOperationException("Not implemented"); //todo this one is long
    }
}
