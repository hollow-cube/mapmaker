package net.hollowcube.mapmaker.runtime.freeform.lua.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.runtime.freeform.lua.base.LuaTextImpl;
import net.minestom.server.entity.Entity;

@LuaType
public class LuaTextDisplayEntity extends LuaDisplayEntity implements LuaTextDisplayEntity$luau {

    public LuaTextDisplayEntity(Entity delegate) {
        super(delegate);
    }

    @Override
    protected DisplayEntity.Text delegate() {
        return (DisplayEntity.Text) super.delegate();
    }

    @Override
    public boolean readField(LuaState state, String key, int index) {
        // Note: Fields supporting interpolation should be added to readInterpField ONLY, not this method also.
        return switch (key) {
            case "Text" -> {
                var text = LuaTextImpl.checkAnyTextArg(state, -1);
                delegate().getEntityMeta().setText(text);
                yield true;
            }
            default -> super.readField(state, key, index);
        };
    }

    @Override
    public boolean readInterpField(LuaState state, String key, int index) {
        return switch (key) {
            default -> super.readInterpField(state, key, index);
        };
    }

    //region Properties

    @LuaProperty
    public int getText(LuaState state) {
        LuaTextImpl.push(state, delegate().getEntityMeta().getText());
        return 1;
    }

    @LuaProperty
    public int setText(LuaState state) {
        var text = LuaTextImpl.checkAnyTextArg(state, 1);
        delegate().getEntityMeta().setText(text);
        return 0;
    }

    @LuaProperty
    public int getTextOpacity(LuaState state) {
        state.pushNumber((delegate().getEntityMeta().getTextOpacity() & 0xFF) / 255f);
        return 1;
    }

    @LuaProperty
    public int setTextOpacity(LuaState state) {
        float opacity = (float) state.checkNumberArg(1);
        if (opacity < 0f || opacity > 1f)
            state.argError(1, "Expected number between 0 and 1 (inclusive)");
        delegate().getEntityMeta().setTextOpacity((byte) Math.round(opacity * 255.0f));
        return 0;
    }

    //endregion

//        * Alignment
//        * Background
//        * DefaultBackground t/f
//        * LineWidth
//        * SeeThrough
//        * Shadow
//        * TextOpacity
}
