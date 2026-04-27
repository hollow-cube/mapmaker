package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.*;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.event.PlayerJumpEvent;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.util.LuaHelpers;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.potion.Potion;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

import static net.hollowcube.mapmaker.scripting.util.LuaHelpers.*;

@LuaLibrary(name = "@mapmaker/player")
public final class LibPlayer {
    // The player library only exports the type itself currently.

    private static final Tag<Player> PLAYER_TAG = Tag.Transient("lua_player");

    @LuaExport
    public static final class Player {
        private final MapPlayer player;

        private @Nullable Sidebar sidebar;

        private Player(MapPlayer player) {
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
        public int getSidebar(LuaState state) {
            if (sidebar == null) {
                sidebar = new Sidebar(this);
                ScriptContext.get(state).track(sidebar);
            }
            LibPlayer$luau.pushSidebar(state, sidebar);
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
        public int getOnInteractBlock(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, PlayerBlockInteractEvent event) {
                    if (event.getHand() != PlayerHand.MAIN) return -1; // ignore
                    LuaVector.push(state, event.getBlockPosition());
                    return 1;
                }
            }

            LibBase.pushEventSource(state, PlayerBlockInteractEvent.class, Impl::pushArgs);
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

        @LuaProperty
        public int getOnUseItem(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, PlayerUseItemEvent event) {
                    if (event.getHand() != PlayerHand.MAIN) return -1; // Ignore
                    return 0;
                }
            }

            LibBase.pushEventSource(state, PlayerUseItemEvent.class, Impl::pushArgs);
            return 1;
        }

        //endregion

        //region Instance Methods

        @LuaMethod
        public void sendMessage(LuaState state) {
            var msg = LuaText.checkAnyText(state, 1);
            player.sendMessage(msg);
        }

        // (self, title: AnyText, subtitle?: AnyText, { fadeIn?: number, stay?: number, fadeOut?: number }?)
        @LuaMethod
        public void showTitle(LuaState state) {
            int top = state.top();
            var title = LuaText.checkAnyText(state, 1);
            var subtitle = top > 1 ? LuaText.checkAnyText(state, 2) : Component.empty();
            // TODO: fade options

            player.showTitle(Title.title(title, subtitle));
        }

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

        // (self, position: vector, yaw: number?, pitch: number?, relativeFlags: 'xyzrw'?)
        @LuaMethod
        public void teleport(LuaState state) {
            int top = state.top();
            var position = LuaVector.check(state, 1);
            boolean hasYaw = top > 1, hasPitch = top > 2;
            float yaw = hasYaw ? (float) state.checkNumber(2) : 0;
            float pitch = hasPitch ? (float) state.checkNumber(3) : 0;
            String flagString = top > 3 ? state.checkString(4) : "";

            int relativeFlags = RelativeFlags.DELTA_COORD; // Dont affect velocity
            for (char c : flagString.toCharArray()) {
                switch (c) {
                    case 'x' -> relativeFlags |= RelativeFlags.X;
                    case 'y' -> relativeFlags |= RelativeFlags.Y;
                    case 'z' -> relativeFlags |= RelativeFlags.Z;
                    case 'r' -> relativeFlags |= RelativeFlags.YAW;
                    case 'w' -> relativeFlags |= RelativeFlags.PITCH;
                    default -> throw state.error("Invalid relative flag '" + c + "', expected 'xyzrw'");
                }
            }
            // If yaw or pitch were provided, dont change them (aka 0 relative to current)
            if (!hasYaw) relativeFlags |= RelativeFlags.YAW;
            if (!hasPitch) relativeFlags |= RelativeFlags.PITCH;

            player.teleport(new Pos(position, yaw, pitch), Vec.ZERO, null, relativeFlags);
        }

        @LuaMethod
        public int getSlot(LuaState state) {
            var slot = LibItem.checkSlot(state, 1);
            LibItem.pushItem(state, player.getEquipment(slot));
            return 1;
        }

        @LuaMethod
        public void setSlot(LuaState state) {
            var slot = LibItem.checkSlot(state, 1);
            var item = LibItem.checkItemArg(state, 2);
            player.setEquipment(slot, item);
        }

        @LuaMethod
        public void setItem(LuaState state) {
            var slot = state.checkInteger(1);
            var item = LibItem.checkItemArg(state, 2);
            player.getInventory().setItemStack(slot, item);
        }

        // (self, effect: string, amplifier?: number, duration?: number)
        @LuaMethod
        public void addEffect(LuaState state) {
            var effect = PotionEffect.fromKey(state.checkString(1));
            if (effect == null) throw state.error("unknown potion effect: " + state.toString(1));
            var amplifier = state.optInteger(2, 1) - 1;
            if (amplifier < 0) amplifier = 0;
            var duration = state.optInteger(3, Potion.INFINITE_DURATION);
            if (duration < Potion.INFINITE_DURATION) duration = Potion.INFINITE_DURATION;

            player.addEffect(new Potion(effect, amplifier, duration, Potion.ICON_FLAG));
        }

        // (self, effect: string)
        @LuaMethod
        public void removeEffect(LuaState state) {
            var effect = PotionEffect.fromKey(state.checkString(1));
            if (effect == null) throw state.error("unknown potion effect: " + state.toString(1));

            player.removeEffect(effect);
        }

        // (self)
        @LuaMethod
        public void clearEffects(LuaState state) {
            player.clearEffects();
        }


        //endregion

        //region Meta Methods

        @LuaMethod(meta = Meta.EQ)
        public int luaToString(LuaState state) {
            var result = state.isUserData(1)
                         && state.toUserData(1) instanceof Player p
                         && p.player.getUuid().equals(player.getUuid());
            state.pushBoolean(result);
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

            if (!tableGet(state, 2, "position"))
                state.argError(2, "Missing position");
            Point point = LuaVector.check(state, -1);
            state.pop(1); // remove position
            float yaw = 0, pitch = 0;
            if (tableGet(state, 2, "yaw")) {
                yaw = (float) state.toNumber(-1);
                state.pop(1); // remove yaw
            }
            if (tableGet(state, 2, "pitch")) {
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

        @LuaMethod
        public void setBlock(LuaState state) {
            var position = LuaVector.check(state, 1);
            var blockState = state.checkString(2);
            var block = Block.fromState(blockState);

            GhostBlockHolder.forPlayer(player).setBlock(position, block);
        }

    }

    @LuaExport
    public static final class Sidebar implements Disposable {
        private final Player player;
        private final net.minestom.server.scoreboard.Sidebar delegate;
        private boolean disposed = false;

        private @Nullable LuaState state;
        private int threadRef = -1;
        private int updateRef = -1;
        private @Nullable Task updateTask;

        public Sidebar(Player player) {
            this.player = player;
            delegate = new net.minestom.server.scoreboard.Sidebar(Component.empty());
            delegate.addViewer(player.player);
        }

        // (self, fn: (Player) -> { title: Text, lines: Text[] })
        @LuaMethod
        public void set(LuaState state) {
            state.checkType(1, LuaType.FUNCTION);

            if (threadRef != -1) {
                state.unref(threadRef);
                state.unref(updateRef);
                updateTask.cancel();
            }

            this.state = state;
            state.pushThread(state);
            threadRef = state.ref(-1);
            state.pop(1);
            updateRef = state.ref(1);

            updateTask = ScriptContext.get(state).scheduler().scheduleTask(() -> {
                if (this.state == null) return TaskSchedule.stop();

                state.getRef(updateRef);
                LibPlayer$luau.pushPlayer(state, player);
                state.call(1, 1);

                state.checkType(-1, LuaType.TABLE);
                if (tableGet(state, -1, "title")) {
                    var title = LuaText.checkAnyText(state, -1);
                    state.pop(1);
                    delegate.setTitle(title);
                }

                for (var line : Set.copyOf(delegate.getLines())) {
                    delegate.removeLine(line.getId());
                }
                if (tableGet(state, -1, "lines")) {
                    state.checkType(-1, LuaType.TABLE);
                    arrayForEach(state, -1, (index) -> {
                        var line = LuaText.checkAnyText(state, -1);
                        delegate.createLine(new net.minestom.server.scoreboard.Sidebar.ScoreboardLine(
                            String.valueOf(index), line, index, net.minestom.server.scoreboard.Sidebar.NumberFormat.blank()));
                    });
                    state.pop(1);
                }

                state.pop(1);

                return TaskSchedule.nextTick();
            }, TaskSchedule.nextTick());
        }

        @Override
        public void dispose() {
            delegate.removeViewer(player.player);

            state.unref(threadRef);
            state.unref(updateRef);
            updateTask.cancel();

            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }

    public static void pushPlayer(LuaState state, net.minestom.server.entity.Player player) {
        // We want to reuse the lua player within a script runtime since it stores some extra state
        var luaPlayer = player.updateAndGetTag(PLAYER_TAG, pp -> {
            if (pp != null) return pp;

            // TODO: This is never cleaned up!
            return new Player((MapPlayer) player);
        });
        LibPlayer$luau.pushPlayer(state, luaPlayer);
    }

    public static MapPlayer checkPlayerArg(LuaState state, int argIndex) {
        return LibPlayer$luau.checkPlayerArg(state, argIndex).player;
    }

}
