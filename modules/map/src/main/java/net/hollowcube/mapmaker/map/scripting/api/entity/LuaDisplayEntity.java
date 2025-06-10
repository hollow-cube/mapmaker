package net.hollowcube.mapmaker.map.scripting.api.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.scripting.api.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.map.scripting.api.world.LuaBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.checkFloat4Arg;
import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.pushFloat4;

public abstract class LuaDisplayEntity extends LuaEntity {

    protected LuaDisplayEntity(@NotNull Entity entity) {
        super(entity);
    }

    private int getTranslation(@NotNull LuaState state) {
        var translation = delegate().getEntityMeta().getTranslation();
        LuaVectorTypeImpl.push(state, translation);
        return 1;
    }

    private int setTranslation(@NotNull LuaState state, int index) {
        var translation = LuaVectorTypeImpl.checkArg(state, index);
        delegate().getEntityMeta().setTranslation(translation);
        return 0;
    }

    private int getScale(@NotNull LuaState state) {
        var scale = delegate().getEntityMeta().getScale();
        LuaVectorTypeImpl.push(state, scale);
        return 1;
    }

    private int setScale(@NotNull LuaState state, int index) {
        var scale = LuaVectorTypeImpl.checkArg(state, index);
        if (scale.x() <= 0 || scale.y() <= 0 || scale.z() <= 0)
            state.error("Scale must be positive");
        delegate().getEntityMeta().setScale(Vec.fromPoint(scale));
        return 0;
    }

    private int getLeftRotation(@NotNull LuaState state) {
        pushFloat4(state, delegate().getEntityMeta().getLeftRotation());
        return 1;
    }

    private int setLeftRotation(@NotNull LuaState state, int index) {
        var rotation = checkFloat4Arg(state, index);
        delegate().getEntityMeta().setLeftRotation(rotation);
        return 0;
    }

    private int getRightRotation(@NotNull LuaState state) {
        pushFloat4(state, delegate().getEntityMeta().getRightRotation());
        return 1;
    }

    private int setRightRotation(@NotNull LuaState state, int index) {
        var rotation = checkFloat4Arg(state, index);
        delegate().getEntityMeta().setRightRotation(rotation);
        return 0;
    }

    private int getBillboard(@NotNull LuaState state) {
        var billboard = delegate().getEntityMeta().getBillboardRenderConstraints();
        state.pushString(billboard.toString().toLowerCase(Locale.ROOT));
        return 1;
    }

    private int setBillboard(@NotNull LuaState state, int index) {
        var billboardRaw = state.checkStringArg(index);
        try {
            var billboard = AbstractDisplayMeta.BillboardConstraints
                    .valueOf(billboardRaw.toUpperCase(Locale.ROOT));
            delegate().getEntityMeta().setBillboardRenderConstraints(billboard);
        } catch (IllegalArgumentException e) {
            state.error("Invalid billboard type: " + billboardRaw);
        }
        return 0;
    }

    private int getGlowColorOverride(@NotNull LuaState state) {
        var color = delegate().getEntityMeta().getGlowColorOverride();
        if (color == -1) state.pushNil();
        else state.pushInteger(color);
        return 1;
    }

    private int setGlowColorOverride(@NotNull LuaState state, int index) {
        int color = state.isNil(index) ? -1 : Math.max(0, state.checkIntegerArg(index));
        delegate().getEntityMeta().setGlowColorOverride(color);
        return 0;
    }

    private int getWidth(@NotNull LuaState state) {
        state.pushNumber(delegate().getEntityMeta().getWidth());
        return 1;
    }

    private int setWidth(@NotNull LuaState state, int index) {
        double width = Math.clamp(state.checkNumberArg(index), 0, 100);
        delegate().getEntityMeta().setWidth((float) width);
        return 0;
    }

    private int getHeight(@NotNull LuaState state) {
        state.pushNumber(delegate().getEntityMeta().getHeight());
        return 1;
    }

    private int setHeight(@NotNull LuaState state, int index) {
        double height = Math.clamp(state.checkNumberArg(index), 0, 100);
        delegate().getEntityMeta().setHeight((float) height);
        return 0;
    }

    private int getInterpolationDuration(@NotNull LuaState state) {
        state.pushInteger(delegate().getEntityMeta().getTransformationInterpolationDuration());
        return 1;
    }

    private int setInterpolationDuration(@NotNull LuaState state, int index) {
        int interpolationDuration = Math.min(0, state.checkIntegerArg(index));
        delegate().getEntityMeta().setTransformationInterpolationDuration(interpolationDuration);
        return 0;
    }

    private int getTeleportDuration(@NotNull LuaState state) {
        state.pushInteger(delegate().getEntityMeta().getPosRotInterpolationDuration());
        return 1;
    }

    private int setTeleportDuration(@NotNull LuaState state, int index) {
        int teleportDuration = Math.clamp(state.checkIntegerArg(index), 0, 59);
        delegate().getEntityMeta().setPosRotInterpolationDuration(teleportDuration);
        return 0;
    }

    private int getStartInterpolation(@NotNull LuaState state) {
        state.pushInteger(delegate().getEntityMeta().getTransformationInterpolationStartDelta());
        return 1;
    }

    private int setStartInterpolation(@NotNull LuaState state, int index) {
        int startInterpolation = Math.max(0, state.checkIntegerArg(index));
        delegate().getEntityMeta().setTransformationInterpolationStartDelta(startInterpolation);
        return 0;
    }

    private int getViewRange(@NotNull LuaState state) {
        state.pushNumber(delegate().getEntityMeta().getViewRange());
        return 1;
    }

    private int setViewRange(@NotNull LuaState state, int index) {
        double viewRange = Math.clamp(state.checkNumberArg(index), 0, 10);
        delegate().getEntityMeta().setViewRange((float) viewRange);
        return 0;
    }

    @Override
    public @NotNull DisplayEntity delegate() {
        return (DisplayEntity) super.delegate();
    }

    @Override
    protected int luaIndex(@NotNull LuaState state, @NotNull String methodName) {
        return switch (methodName) {
            case "Translation" -> getTranslation(state);
            case "Scale" -> getScale(state);
            case "LeftRotation" -> getLeftRotation(state);
            case "RightRotation" -> getRightRotation(state);
            case "Billboard" -> getBillboard(state);
            case "GlowColorOverride" -> getGlowColorOverride(state);
            case "Width" -> getWidth(state);
            case "Height" -> getHeight(state);
            case "InterpolationDuration" -> getInterpolationDuration(state);
            case "TeleportDuration" -> getTeleportDuration(state);
            case "StartInterpolation" -> getStartInterpolation(state);
            case "ViewRange" -> getViewRange(state);
            default -> super.luaIndex(state, methodName);
        };
    }

    @Override
    protected int luaNewIndex(@NotNull LuaState state, @NotNull String methodName) {
        return switch (methodName) {
            case "Translation" -> setTranslation(state, 1);
            case "Scale" -> setScale(state, 1);
            case "LeftRotation" -> setLeftRotation(state, 1);
            case "RightRotation" -> setRightRotation(state, 1);
            case "Billboard" -> setBillboard(state, 1);
            case "GlowColorOverride" -> setGlowColorOverride(state, 1);
            case "Width" -> setWidth(state, 1);
            case "Height" -> setHeight(state, 1);
            case "InterpolationDuration" -> setInterpolationDuration(state, 1);
            case "TeleportDuration" -> setTeleportDuration(state, 1);
            case "StartInterpolation" -> setStartInterpolation(state, 1);
            case "ViewRange" -> setViewRange(state, 1);
            default -> super.luaNewIndex(state, methodName);
        };
    }

    @Override
    protected int readFieldFromTable(@NotNull LuaState state, @NotNull String fieldName) {
        return switch (fieldName) {
            case "Translation" -> setTranslation(state, -1);
            case "Scale" -> setScale(state, -1);
            case "LeftRotation" -> setLeftRotation(state, -1);
            case "RightRotation" -> setRightRotation(state, -1);
            case "Billboard" -> setBillboard(state, -1);
            case "GlowColorOverride" -> setGlowColorOverride(state, -1);
            case "Width" -> setWidth(state, -1);
            case "Height" -> setHeight(state, -1);
            case "InterpolationDuration" -> setInterpolationDuration(state, -1);
            case "TeleportDuration" -> setTeleportDuration(state, -1);
            case "StartInterpolation" -> setStartInterpolation(state, -1);
            case "ViewRange" -> setViewRange(state, -1);
            default -> super.readFieldFromTable(state, fieldName);
        };
    }

    public static class Block extends LuaDisplayEntity {

        public Block(@Nullable UUID uuid) {
            super(new DisplayEntity.Block(Objects.requireNonNullElseGet(uuid, UUID::randomUUID)));
        }

        private int getBlockState(@NotNull LuaState state) {
            LuaBlock.push(state, delegate().getEntityMeta().getBlockStateId());
            return 1;
        }

        private int setBlockState(@NotNull LuaState state, int index) {
            var block = LuaBlock.checkArg(state, index);
            delegate().getEntityMeta().setBlockState(block);
            return 0;
        }

        @Override
        public @NotNull DisplayEntity.Block delegate() {
            return (DisplayEntity.Block) super.delegate();
        }

        @Override
        protected int luaIndex(@NotNull LuaState state, @NotNull String methodName) {
            return switch (methodName) {
                case "BlockState" -> getBlockState(state);
                default -> super.luaIndex(state, methodName);
            };
        }

        @Override
        protected int luaNewIndex(@NotNull LuaState state, @NotNull String methodName) {
            return switch (methodName) {
                case "BlockState" -> setBlockState(state, 1);
                default -> super.luaNewIndex(state, methodName);
            };
        }

        @Override
        protected int readFieldFromTable(@NotNull LuaState state, @NotNull String fieldName) {
            return switch (fieldName) {
                case "BlockState" -> setBlockState(state, -1);
                default -> super.readFieldFromTable(state, fieldName);
            };
        }
    }

    public static class Item extends LuaDisplayEntity {

        public Item(@Nullable UUID uuid) {
            super(new DisplayEntity.Item(Objects.requireNonNullElseGet(uuid, UUID::randomUUID)));
        }

        @Override
        public @NotNull DisplayEntity.Item delegate() {
            return (DisplayEntity.Item) super.delegate();
        }

    }

    public static class Text extends LuaDisplayEntity {

        public Text(@Nullable UUID uuid) {
            super(new DisplayEntity.Text(Objects.requireNonNullElseGet(uuid, UUID::randomUUID)));
        }

        private int getAlignment(@NotNull LuaState state) {
            boolean left = delegate().getEntityMeta().isAlignLeft();
            boolean right = delegate().getEntityMeta().isAlignRight();
            if (left && !right) state.pushString("left");
            else if (right && !left) state.pushString("right");
            else state.pushString("center");
            return 1;
        }

        private int setAlignment(@NotNull LuaState state, int index) {
            var alignment = state.checkStringArg(index);
            var meta = delegate().getEntityMeta();
            boolean left = "left".equals(alignment),
                    right = "right".equals(alignment),
                    center = "center".equals(alignment);
            if (!left && !right && !center)
                state.error("alignment must be 'left', 'right', or 'center'");
            meta.setAlignLeft(left || center);
            meta.setAlignRight(right || center);
            return 0;
        }

        private int getBackground(@NotNull LuaState state) {
            state.pushInteger(delegate().getEntityMeta().getBackgroundColor());
            return 1;
        }

        private int setBackground(@NotNull LuaState state, int index) {
            int color = state.checkIntegerArg(index);
            delegate().getEntityMeta().setBackgroundColor(color);
            return 0;
        }

        private int getLineWidth(@NotNull LuaState state) {
            state.pushNumber(delegate().getEntityMeta().getLineWidth());
            return 1;
        }

        private int setLineWidth(@NotNull LuaState state, int index) {
            double lineWidth = Math.min(0, state.checkNumberArg(index));
            return 0;
        }

        private int getSeeThrough(@NotNull LuaState state) {
            state.pushBoolean(delegate().getEntityMeta().isSeeThrough());
            return 1;
        }

        private int setSeeThrough(@NotNull LuaState state, int index) {
            boolean seeThrough = state.checkBooleanArg(index);
            delegate().getEntityMeta().setSeeThrough(seeThrough);
            return 0;
        }

        private int getShadow(@NotNull LuaState state) {
            var shadow = delegate().getEntityMeta().isShadow();
            state.pushBoolean(shadow);
            return 1;
        }

        private int setShadow(@NotNull LuaState state, int index) {
            boolean shadow = state.checkBooleanArg(index);
            delegate().getEntityMeta().setShadow(shadow);
            return 0;
        }

        private int getText(@NotNull LuaState state) {
            // TODO text components!
            var text = delegate().getEntityMeta().getText();
            var string = PlainTextComponentSerializer.plainText().serialize(text);
            state.pushString(string);
            return 1;
        }

        private int setText(@NotNull LuaState state, int index) {
            var text = state.checkStringArg(index);
            delegate().getEntityMeta().setText(Component.text(text));
            return 0;
        }

        private int getTextOpacity(@NotNull LuaState state) {
            int opacity = delegate().getEntityMeta().getTextOpacity();
            state.pushNumber(opacity / 255f);
            return 1;
        }

        private int setTextOpacity(@NotNull LuaState state, int index) {
            double opacity = Math.clamp(state.checkNumberArg(index), 0, 1);
            byte opacityByte = (byte) (opacity * 255);
            delegate().getEntityMeta().setTextOpacity(opacityByte);
            return 0;
        }

        @Override
        public @NotNull DisplayEntity.Text delegate() {
            return (DisplayEntity.Text) super.delegate();
        }

        @Override
        protected int luaIndex(@NotNull LuaState state, @NotNull String methodName) {
            return switch (methodName) {
                case "Alignment" -> getAlignment(state);
                case "Background" -> getBackground(state);
                case "LineWidth" -> getLineWidth(state);
                case "SeeThrough" -> getSeeThrough(state);
                case "Shadow" -> getShadow(state);
                case "Text" -> getText(state);
                case "TextOpacity" -> getTextOpacity(state);
                default -> super.luaIndex(state, methodName);
            };
        }

        @Override
        protected int luaNewIndex(@NotNull LuaState state, @NotNull String methodName) {
            return switch (methodName) {
                case "Alignment" -> setAlignment(state, 1);
                case "Background" -> setBackground(state, 1);
                case "LineWidth" -> setLineWidth(state, 1);
                case "SeeThrough" -> setSeeThrough(state, 1);
                case "Shadow" -> setShadow(state, 1);
                case "Text" -> setText(state, 1);
                case "TextOpacity" -> setTextOpacity(state, 1);
                default -> super.luaNewIndex(state, methodName);
            };
        }

        @Override
        protected int readFieldFromTable(@NotNull LuaState state, @NotNull String fieldName) {
            return switch (fieldName) {
                case "Alignment" -> setAlignment(state, -1);
                case "Background" -> setBackground(state, -1);
                case "LineWidth" -> setLineWidth(state, -1);
                case "SeeThrough" -> setSeeThrough(state, -1);
                case "Shadow" -> setShadow(state, -1);
                case "Text" -> setText(state, -1);
                case "TextOpacity" -> setTextOpacity(state, -1);
                default -> super.readFieldFromTable(state, fieldName);
            };
        }
    }

}
