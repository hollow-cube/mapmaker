package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.event.PlayerJumpEvent;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.util.LuaHelpers;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.entity.EntityAttackEvent;

import java.util.UUID;

import static net.hollowcube.mapmaker.scripting.util.LuaHelpers.*;

@LuaLibrary(name = "@mapmaker/player")
public final class LibPlayer {
    // The player library only exports the type itself currently.

    @LuaExport
    public static final class Player {
        private final MapPlayer player;

        Player(MapPlayer player) {
            this.player = player;
        }

        //region Properties

        @LuaProperty
        public int getUuid(LuaState state) {
            state.pushString(player.getUuid().toString());
            return 1;
        }

        @LuaProperty
        public int getName(LuaState state) {
            state.pushString(player.getUsername());
            return 1;
        }

        @LuaProperty
        public int getPosition(LuaState state) {
            var pos = player.getPosition();
            state.pushVector((float) pos.x(), (float) pos.y(), (float) pos.z());
            return 1;
        }

        @LuaProperty
        public int getYaw(LuaState state) {
            state.pushNumber(player.getPosition().yaw());
            return 1;
        }

        @LuaProperty
        public int getPitch(LuaState state) {
            state.pushNumber(player.getPosition().pitch());
            return 1;
        }

        @LuaProperty
        public int getWorld(LuaState state) {
            LibPlayer$luau.pushWorldView(state, new WorldView(player));
            return 1;
        }

        @LuaProperty
        public int getDeprecated_scale(LuaState state) {
            state.pushNumber(player.getAttributeValue(Attribute.SCALE));
            return 1;
        }

        //endregion

        //region Events

        @LuaProperty
        public int getOnHitPlayer(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, EntityAttackEvent event) {
                    if (!(event.getTarget() instanceof MapPlayer other))
                        return -1;

                    pushPlayer(state, other);
                    return 1;
                }
            }

            LibBase.pushEventSource(state, EntityAttackEvent.class, Impl::pushArgs);
            return 1;
        }

        @LuaProperty
        public int getOnJump(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, PlayerJumpEvent event) {
                    return 0;
                }
            }

            LibBase.pushEventSource(state, PlayerJumpEvent.class, Impl::pushArgs);
            return 1;
        }

        //endregion

        //region Instance Methods

        // (self, sound: string, options: SoundOptions?) -> ()
        @LuaMethod
        public void playSound(LuaState state) {
            var sound = Sound.sound();
            sound.type(checkKey(state, 1));

            readSoundOptions(state, sound, 2);

            player.playSound(sound.build(), Sound.Emitter.self());
        }

        // (self, sound: string, position: vector, options: SoundOptions?) -> ()
        @LuaMethod
        public void playSoundAt(LuaState state) {
            var sound = Sound.sound();
            sound.type(checkKey(state, 1));
            var pos = LuaVector.check(state, 2);
            readSoundOptions(state, sound, 3);

            player.playSound(sound.build(), pos);
        }

        // (self, sound: string?, category: SoundCategory?) -> ()
        @LuaMethod
        public void stopSound(LuaState state) {
            int top = state.top();
            var sound = top > 0 ? checkOptKey(state, 1) : null;
            var source = top > 1 ? checkOptSoundCategory(state, 2) : null;

            SoundStop stop;
            if (sound != null && source != null)
                stop = SoundStop.namedOnSource(sound, source);
            else if (sound != null)
                stop = SoundStop.named(sound);
            else if (source != null)
                stop = SoundStop.source(source);
            else stop = SoundStop.all();
            player.stopSound(stop);
        }

        private void readSoundOptions(LuaState state, Sound.Builder sound, int index) {
            if (state.top() < index) return;

            state.checkType(index, LuaType.TABLE);
            if (tableGet(state, index, "volume")) {
                sound.volume((float) Math.clamp(state.checkNumber(-1), 0, 1));
                state.pop(1);
            }
            if (tableGet(state, index, "pitch")) {
                sound.pitch((float) Math.clamp(state.checkNumber(-1), 0, 2));
                state.pop(1);
            }
            if (tableGet(state, index, "category")) {
                sound.source(checkSoundCategory(state, -1));
                state.pop(1);
            }
        }

        @LuaMethod
        public int getSlot(LuaState state) {
            var slot = LibItem.checkSlot(state, 1);
            LibItem.pushItem(state, player.getEquipment(slot));
            return 1;
        }

        //endregion
    }

    @LuaExport
    public static final class WorldView {
        private final MapPlayer player;

        WorldView(MapPlayer player) {
            this.player = player;
        }

        @LuaMethod
        public int spawnEntity(LuaState state) {
            var typeName = state.checkString(1); // entity type
            state.checkType(2, LuaType.TABLE); // init

            if (!typeName.equals("text"))
                throw state.error("Only text entity is supported");

            var entity = new DisplayEntity.Text(UUID.randomUUID());
            entity.setAutoViewable(false);
//            entity.updateViewerRule(other -> other == player);

            var luaEntity = new LibEntity.TextDisplay(entity);
            LuaHelpers.tableForEach(state, 2, (key) -> {
                if ("position".equals(key) || "yaw".equals(key) || "pitch".equals(key))
                    return; // Special handling below
                if (!luaEntity.readField(state, key, -1)) {
                    state.argError(2, "Unknown property: " + key);
                }
            });

            if (!LuaHelpers.tableGet(state, 2, "position"))
                state.argError(2, "Missing position");
            Point point = LuaVector.check(state, -1);
            state.pop(1); // remove position
            float yaw = 0, pitch = 0;
            if (LuaHelpers.tableGet(state, 2, "yaw")) {
                yaw = (float) state.toNumber(-1);
                state.pop(1); // remove yaw
            }
            if (LuaHelpers.tableGet(state, 2, "pitch")) {
                pitch = (float) state.toNumber(-1);
                state.pop(1); // remove position
            }

            entity.setInstance(player.getInstance(), new Pos(point, yaw, pitch));
            entity.addViewer(player);
            ScriptContext.get(state).track(new Disposable() {
                @Override
                public void dispose() {
                    entity.remove();
                }

                @Override
                public boolean isDisposed() {
                    return entity.isRemoved();
                }
            });

            LibEntity.pushEntity(state, luaEntity);
            return 1;
        }

    }

    public static void pushPlayer(LuaState state, net.minestom.server.entity.Player player) {
        LibPlayer$luau.pushPlayer(state, new Player((MapPlayer) player));
    }

    public static MapPlayer checkPlayerArg(LuaState state, int argIndex) {
        return LibPlayer$luau.checkPlayerArg(state, argIndex).player;
    }

}
