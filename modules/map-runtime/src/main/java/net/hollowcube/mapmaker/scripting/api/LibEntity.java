package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.scripting.util.LuaHelpers;
import net.hollowcube.scripting.gen.*;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;

import java.util.Locale;

/// Entity types. To spawn an entity, call `player.world:spawn_entity(...)`.
@LuaLibrary(name = "@mapmaker/entity")
public final class LibEntity {

    public static void pushProp(LuaState state, Prop prop) {
        LibEntity$luau.pushProp(state, prop);
    }

    /// The base type for all props.
    @LuaExport
    @LuaUnion(discriminator = "kind")
    public static sealed abstract class Prop permits TextProp {
        private final DisplayEntity delegate;

        protected Prop(DisplayEntity delegate) {
            this.delegate = delegate;
        }

        protected DisplayEntity delegate() {
            return this.delegate;
        }

        public boolean readField(LuaState state, String key, int index) {
            // TODO: should support extra string atoms added via annotations as constants (eg LibEntity$luau.ATOM_BILLBOARD)
            switch (key) {
                /* position, yaw, pitch have special cases in spawnEntity */
                case "billboard" -> setBillboard(state, index);
                default -> {
                    return readInterpField(state, key, index);
                }
            }
            return true;
        }

        public boolean readInterpField(LuaState state, String key, int index) {
            switch (key) {
                case "scale" -> setScale(state, index);
                default -> {
                    return false;
                }
            }
            return true;
        }

        //region Properties

        /// The prop's current position in the world.
        /// @luaReturn vector
        @LuaProperty
        public int getPosition(LuaState state) {
            var pos = delegate().getPosition();
            state.pushVector((float) pos.x(), (float) pos.y(), (float) pos.z());
            return 1;
        }

        /// The prop's yaw (horizontal rotation), in degrees.
        /// @luaReturn number
        @LuaProperty
        public int getYaw(LuaState state) {
            state.pushNumber(delegate().getPosition().yaw());
            return 1;
        }

        /// The prop's pitch (vertical rotation), in degrees.
        /// @luaReturn number
        @LuaProperty
        public int getPitch(LuaState state) {
            state.pushNumber(delegate().getPosition().pitch());
            return 1;
        }

        // TODO: block light, sky light, glow color override, width, height, shadow radius, shadow strength, view range, translation

        /// The display's scale. Each axis can be set independently. Interpolatable.
        ///
        /// @luaReturn vector
        @LuaProperty
        public int getScale(LuaState state) {
            LuaVector.push(state, delegate().getEntityMeta().getScale());
            return 1;
        }

        /// @luaParam value vector
        @LuaProperty
        public void setScale(LuaState state) {
            setScale(state, 1);
        }

        private void setScale(LuaState state, int index) {
            var scale = LuaVector.check(state, index);
            delegate().getEntityMeta().setScale(scale.asVec());
        }

        // TODO: leftRotation, rightRotation, transformation

        /// How the prop orients itself toward the player.
        ///
        /// @luaReturn "fixed" | "vertical" | "horizontal" | "center"
        @LuaProperty
        public int getBillboard(LuaState state) {
            state.pushString(delegate().getEntityMeta().getBillboardRenderConstraints().name().toLowerCase(Locale.ROOT));
            return 1;
        }

        /// @luaParam value "fixed" | "vertical" | "horizontal" | "center"
        @LuaProperty
        public void setBillboard(LuaState state) {
            setBillboard(state, 1);
        }

        private void setBillboard(LuaState state, int index) {
            // TODO: its kinda weird to use argErrors here since we arent necessarily in an argument (ie readField)
            //       but not totally sure what to do with those errors now so will leave it for now.
            //       the errors will be pretty gross tho (referencing arg #-1)
            var billboardString = state.checkString(index);
            try {
                var billboard = AbstractDisplayMeta.BillboardConstraints.valueOf(billboardString.toUpperCase(Locale.ROOT));
                delegate().getEntityMeta().setBillboardRenderConstraints(billboard);
            } catch (IllegalArgumentException e) {
                state.argError(index, "Invalid billboard value, must be one of 'fixed', 'vertical', 'horizontal', or 'center'");
            }
        }

        //endregion

        //region Instance Methods

        /// Smoothly transitions properties to new values over `duration` ticks. Only
        /// interpolatable properties can appear in the target table.
        ///
        /// ```luau
        /// prop:interpolate(20, { scale = vector(2, 2, 2) })  -- grow over one second
        /// ```
        ///
        /// @luaParam duration number
        /// @luaParam properties { [string]: any }
        /// @luaReturn nil
        @LuaMethod
        public int interpolate(LuaState state) {
            int duration = state.checkInteger(1);
            if (duration <= 0) state.argError(1, "must be a positive integer");
            state.checkType(2, LuaType.TABLE);

            // TODO: actually set interpolation flags
            delegate().editEntityMeta(AbstractDisplayMeta.class, meta -> {
                meta.setTransformationInterpolationStartDelta(0); // begin interpolation now
                if (meta.getTransformationInterpolationDuration() != duration)
                    meta.setTransformationInterpolationDuration(duration);

                LuaHelpers.tableForEach(state, 2, (key) -> {
                    if (!readInterpField(state, key, -1)) {
                        throw state.error("Unknown interpolation property: " + key);
                    }
                });
            });

            return 0;
        }

        /// Removes the entity from the world. Has no effect if the entity is already removed.
        @LuaMethod
        public void remove(LuaState state) {
            if (delegate.isRemoved())
                return;

            delegate.remove();
        }

        //endregion
    }

    /// A display that renders styled text floating in the world.
    @LuaExport
    public static final class TextProp extends Prop {

        public TextProp(DisplayEntity delegate) {
            super(delegate);
        }

        @Override
        protected DisplayEntity.Text delegate() {
            return (DisplayEntity.Text) super.delegate();
        }

        @Override
        public boolean readField(LuaState state, String key, int index) {
            // Note: Fields supporting interpolation should be added to readInterpField ONLY, not this method also.
            switch (key) {
                case "text" -> setText(state, index);
                case "shadow" -> setShadow(state, index);
                case "background" -> setBackground(state, index);
                case "opacity" -> setOpacity(state, index);
                default -> {
                    return super.readField(state, key, index);
                }
            }
            return true;
        }

        @Override
        public boolean readInterpField(LuaState state, String key, int index) {
            switch (key) {
                case "$$$TEMP" -> {
                }
                default -> {
                    return super.readInterpField(state, key, index);
                }
            }
            return true;
        }

        //region Properties

        /// @luaReturn "text"
        @LuaProperty
        public int getKind(LuaState state) {
            state.pushString("text");
            return 1;
        }

        /// The text shown by this display.
        /// @luaReturn Text
        @LuaProperty
        public int getText(LuaState state) {
            var text = delegate().getEntityMeta().getText();
            LuaText.push(state, text);
            return 1;
        }

        /// @luaParam value AnyText
        @LuaProperty
        public void setText(LuaState state) {
            setText(state, 1);
        }

        private void setText(LuaState state, int index) {
            var text = LuaText.checkAnyText(state, index);
            delegate().getEntityMeta().setText(text);
        }

        /// Whether the text is rendered with a drop shadow.
        /// @luaReturn boolean
        @LuaProperty
        public int getShadow(LuaState state) {
            state.pushBoolean(delegate().getEntityMeta().isShadow());
            return 1;
        }

        /// @luaParam value boolean
        @LuaProperty
        public void setShadow(LuaState state) {
            setShadow(state, 1);
        }

        private void setShadow(LuaState state, int index) {
            var shadow = state.checkBoolean(index);
            delegate().getEntityMeta().setShadow(shadow);
        }

        /// The background colour behind the text, as a packed ARGB integer.
        /// @luaReturn number
        @LuaProperty
        public int getBackground(LuaState state) {
            int background = delegate().getEntityMeta().getBackgroundColor();
            state.pushInteger(background);
            return 1;
        }

        /// @luaParam value number
        @LuaProperty
        public void setBackground(LuaState state) {
            setBackground(state, 1);
        }

        private void setBackground(LuaState state, int index) {
            // todo: support colors properly
            int background = state.checkInteger(index);
            delegate().getEntityMeta().setBackgroundColor(background);
        }

        // TODO: defaultBackground, line width, see through, shadow, text

        /// The text's opacity, between `0` (fully transparent) and `1` (fully opaque).
        /// @luaReturn number
        @LuaProperty
        public int getOpacity(LuaState state) {
            var opacity = delegate().getEntityMeta().getTextOpacity();
            state.pushNumber((opacity & 0xFF) / 255f);
            return 1;
        }

        /// @luaParam value number
        @LuaProperty
        public void setOpacity(LuaState state) {
            setOpacity(state, 1);
        }

        private void setOpacity(LuaState state, int index) {
            float opacity = (float) state.checkNumber(index);
            if (opacity < 0f || opacity > 1f)
                state.argError(index, "Expected number between 0 and 1 (inclusive)");
            delegate().getEntityMeta().setTextOpacity((byte) Math.round(opacity * 255.0f));
        }

        //endregion

    }

}
