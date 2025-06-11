package net.hollowcube.mapmaker.map.scripting.api.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.scripting.api.math.LuaVectorTypeImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchKey;
import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.noSuchMethod;

public class LuaEntity {
    private static final String NAME = "Entity";

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Entity
        state.newMetaTable(NAME);
        state.pushCFunction(LuaEntity::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaEntity::luaNewIndex, "__newindex");
        state.setField(-2, "__newindex");
        state.pushCFunction(LuaEntity::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(@NotNull LuaState state, @NotNull LuaEntity entity) {
        state.newUserData(entity);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static <E extends LuaEntity> @NotNull E checkArg(@NotNull LuaState state, int index, @NotNull Class<E> type) {
        var entity = (LuaEntity) state.checkUserDataArg(index, NAME);
        if (!type.isAssignableFrom(entity.getClass()))
            state.argError(index, "Expected " + type.getSimpleName() + ", got " + entity.getClass().getSimpleName());
        return type.cast(entity);
    }

    private final Entity delegate;

    public LuaEntity(@NotNull Entity entity) {
        this.delegate = entity;
    }

    public @NotNull Entity delegate() {
        return delegate;
    }

    // Properties

    private int getUuid(@NotNull LuaState state) {
        state.pushString(delegate().getUuid().toString());
        return 1;
    }

    private int getPosition(@NotNull LuaState state) {
        LuaVectorTypeImpl.push(state, delegate().getPosition());
        return 1;
    }

    private int getYaw(@NotNull LuaState state) {
        state.pushNumber(delegate.getPosition().yaw());
        return 1;
    }

    private int getPitch(@NotNull LuaState state) {
        state.pushNumber(delegate.getPosition().pitch());
        return 1;
    }

    // Methods

    ///  Teleport(pos: vector, yaw: number?, pitch: number?, relativeFlags: string?)
    private int teleport(@NotNull LuaState state) {
        int top = state.getTop();
        Point pos = LuaVectorTypeImpl.checkArg(state, 1);

        float yaw, pitch;
        int relFlags = 0;
        if (top > 1 && !state.isNil(2)) {
            yaw = (float) state.checkNumberArg(2);
        } else {
            yaw = 0;
            relFlags |= RelativeFlags.YAW;
        }
        if (top > 2 && !state.isNil(3)) {
            pitch = (float) state.checkNumberArg(3);
        } else {
            pitch = 0;
            relFlags |= RelativeFlags.PITCH;
        }
        if (top > 3) {
            for (char c : state.checkStringArg(4).toCharArray()) {
                int flag = switch (c) {
                    case 'x' -> RelativeFlags.X;
                    case 'y' -> RelativeFlags.Y;
                    case 'z' -> RelativeFlags.Z;
                    case 'r' -> RelativeFlags.YAW;
                    case 'p' -> RelativeFlags.PITCH;
                    default -> {
                        state.argError(4, "Unknown relative teleport flag: " + c);
                        yield 0; // unreachable
                    }
                };
                relFlags |= flag;
            }
        }

        // todo ensure the target position is inside the world border (including bounding box)
        delegate.teleport(new Pos(pos, yaw, pitch), null, relFlags);
        return 0;
    }

    private int remove(@NotNull LuaState state) {
        if (!delegate.isRemoved())
            delegate.remove();
        return 0;
    }

    // Metamethods

    protected int luaIndex(@NotNull LuaState state, @NotNull String methodName) {
        return switch (methodName) {
            case "Uuid" -> getUuid(state);
            case "Position" -> getPosition(state);
            default -> noSuchKey(state, NAME, methodName);
        };
    }

    protected int luaNewIndex(@NotNull LuaState state, @NotNull String methodName) {
        return switch (methodName) {
            default -> noSuchKey(state, NAME, methodName);
        };
    }

    protected int readFieldFromTable(@NotNull LuaState state, @NotNull String fieldName) {
        return switch (fieldName) {
            default -> noSuchKey(state, NAME, fieldName);
        };
    }

    protected int luaNameCall(@NotNull LuaState state, @NotNull String methodName) {
        return switch (methodName) {
            case "Teleport" -> teleport(state);
            case "Remove" -> remove(state);
            default -> noSuchMethod(state, NAME, methodName);
        };
    }

    private static int luaIndex(@NotNull LuaState state) {
        final LuaEntity entity = checkArg(state, 1, LuaEntity.class);
        final String key = state.checkStringArg(2);
        return entity.luaIndex(state, key);
    }

    private static int luaNewIndex(@NotNull LuaState state) {
        final LuaEntity entity = checkArg(state, 1, LuaEntity.class);
        final String key = state.checkStringArg(2);
        state.remove(1); // Remove the userdata from the stack
        state.remove(1); // Remove the key from the stack
        return entity.luaNewIndex(state, key);
    }

    private static int luaNameCall(@NotNull LuaState state) {
        final LuaEntity entity = checkArg(state, 1, LuaEntity.class);
        state.remove(1); // Remove the player userdata from the stack
        final String methodName = state.nameCallAtom();
        return entity.luaNameCall(state, methodName);
    }

}
