package net.hollowcube.mapmaker.map.scripting.api.entity;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import org.jetbrains.annotations.NotNull;

public abstract class LuaDisplayEntity extends LuaEntity {
    public LuaDisplayEntity(@NotNull DisplayEntity delegate) {
        super(delegate);
    }

    @Override
    public @NotNull DisplayEntity delegate() {
        return (DisplayEntity) super.delegate();
    }

    // Metamethods

    protected static int luaIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
        return switch (key) {
            default -> LuaEntity.luaIndex(state, typeName, entity, key);
        };
    }

    protected static int luaNewIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
        return switch (key) {
            default -> LuaEntity.luaNewIndex(state, typeName, entity, key);
        };
    }

    protected static int luaCall(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
        return switch (methodName) {
            default -> LuaEntity.luaNameCall(state, typeName, entity, methodName);
        };
    }

    public static class Block extends LuaDisplayEntity {
        public static final String NAME = "BlockDisplay";

        public Block(@NotNull DisplayEntity.Block delegate) {
            super(delegate);
        }

        @Override
        public @NotNull DisplayEntity.Block delegate() {
            return (DisplayEntity.Block) super.delegate();
        }

        // Metamethods

        protected static int luaIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String methodName) {
            return switch (methodName) {
                default -> LuaDisplayEntity.luaIndex(state, typeName, entity, methodName);
            };
        }

        protected static int luaNewIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
            return switch (key) {
                default -> LuaDisplayEntity.luaNewIndex(state, typeName, entity, key);
            };
        }

        protected static int luaCall(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
            return switch (methodName) {
                default -> LuaDisplayEntity.luaCall(state, typeName, entity, methodName);
            };
        }
    }

    public static class Item extends LuaDisplayEntity {
        public static final String NAME = "ItemDisplay";

        public Item(@NotNull DisplayEntity.Text delegate) {
            super(delegate);
        }

        @Override
        public @NotNull DisplayEntity.Item delegate() {
            return (DisplayEntity.Item) super.delegate();
        }

        // Metamethods

        protected static int luaIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
            return switch (key) {
                default -> LuaDisplayEntity.luaIndex(state, typeName, entity, key);
            };
        }

        protected static int luaNewIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
            return switch (key) {
                default -> LuaDisplayEntity.luaNewIndex(state, typeName, entity, key);
            };
        }

        protected static int luaCall(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
            return switch (methodName) {
                default -> LuaDisplayEntity.luaCall(state, typeName, entity, methodName);
            };
        }
    }

    public static class Text extends LuaDisplayEntity {
        public static final String NAME = "TextDisplay";

        public Text(@NotNull DisplayEntity.Text delegate) {
            super(delegate);
        }

        @Override
        public @NotNull DisplayEntity.Text delegate() {
            return (DisplayEntity.Text) super.delegate();
        }

        // Metamethods

        protected static int luaIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
            return switch (key) {
                default -> LuaDisplayEntity.luaIndex(state, typeName, entity, key);
            };
        }

        protected static int luaNewIndex(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaDisplayEntity entity, @NotNull String key) {
            return switch (key) {
                default -> LuaDisplayEntity.luaNewIndex(state, typeName, entity, key);
            };
        }

        protected static int luaCall(@NotNull LuaState state, @NotNull String typeName, @NotNull LuaEntity entity, @NotNull String methodName) {
            return switch (methodName) {
                default -> LuaDisplayEntity.luaCall(state, typeName, entity, methodName);
            };
        }

        private static int luaIndex(@NotNull LuaState state) {
            final Text entity = checkArg(state, 1);
            final String key = state.checkStringArg(2);
            return luaIndex(state, NAME, entity, key);
        }

        private static int luaNewIndex(@NotNull LuaState state) {
            final Text entity = checkArg(state, 1);
            final String key = state.checkStringArg(2);
            state.remove(1); // Remove the userdata from the stack
            state.remove(1); // Remove the key from the stack
            return luaNewIndex(state, NAME, entity, key);
        }

        private static int luaNameCall(@NotNull LuaState state) {
            final Text entity = checkArg(state, 1);
            state.remove(1); // Remove the player userdata from the stack
            final String methodName = state.nameCallAtom();
            return luaNameCall(state, NAME, entity, methodName);
        }
    }

}
