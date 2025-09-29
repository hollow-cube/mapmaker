package net.hollowcube.mapmaker.runtime.freeform.lua.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.runtime.freeform.script.LuaHelpers;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;

import java.util.Locale;

@LuaType
public class LuaDisplayEntity extends LuaEntity implements LuaDisplayEntity$luau {

    public LuaDisplayEntity(Entity delegate) {
        super(delegate);
    }

    @Override
    protected DisplayEntity delegate() {
        return (DisplayEntity) super.delegate();
    }

    @Override
    public boolean readField(LuaState state, String key, int index) {
        return switch (key) {
            default -> readInterpField(state, key, index)
                    || super.readField(state, key, index);
        };
    }

    public boolean readInterpField(LuaState state, String key, int index) {
        // Note that these keys also need to be added to readField
        return switch (key) {
            default -> false;
        };
    }

    //region Properties

    @LuaProperty
    public int getBillboard(LuaState state) {
        state.pushString(delegate().getEntityMeta().getBillboardRenderConstraints().name().toLowerCase(Locale.ROOT));
        return 1;
    }

    @LuaProperty
    public int setBillboard(LuaState state) {
        var billboardString = state.checkStringArg(1);
        try {
            var billboard = AbstractDisplayMeta.BillboardConstraints.valueOf(billboardString.toUpperCase(Locale.ROOT));
            delegate().getEntityMeta().setBillboardRenderConstraints(billboard);
        } catch (IllegalArgumentException e) {
            state.argError(1, "Invalid billboard value, must be one of 'fixed', 'vertical', 'horizontal', or 'center'");
        }
        return 0;
    }

    // todo block/sky light, dnc for now

    //endregion

    //region Instance Methods

    public int interpolate(LuaState state) {
        int duration = state.checkIntegerArg(1);
        if (duration <= 0) state.argError(1, "must be a positive integer");

        LuaHelpers.tableForEach(state, 2, (key) -> {
            if (!readInterpField(state, key, -1)) {
                state.error("Unknown property for interpolation: " + key);
            }
        });

        return 0;
    }

    //endregion

    /*

     * GlowColorOverride
     * Width/Height
     * InterpolationDuration
     * ShadowRadius
     * ShadowStrength
     * Transformation

     */

}
