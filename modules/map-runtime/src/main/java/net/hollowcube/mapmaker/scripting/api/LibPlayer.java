package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.event.PlayerJumpEvent;
import net.hollowcube.scripting.gen.LuaExport;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.gen.LuaMethod;
import net.hollowcube.scripting.gen.LuaProperty;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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
import net.minestom.server.scoreboard.Sidebar;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import static net.hollowcube.mapmaker.scripting.util.LuaHelpers.*;

/// The `Player` type and per-player helpers like `Sidebar` and `WorldView`.
@LuaLibrary(name = "@mapmaker/player")
public final class LibPlayer {
    // The player library only exports the type itself currently.

    private static final Tag<Player> PLAYER_TAG = Tag.Transient("lua_player");

    /// A player connected to the map.
    @LuaExport
    public static final class Player {
        private final MapPlayer player;

        private @Nullable Sidebar sidebar;

        private Player(MapPlayer player) {
            this.player = player;
        }

        //region Properties

        /// The player's UUID.
        ///
        /// @luaReturn string
        @LuaProperty
        public int getUuid(LuaState state) {
            state.pushString(player.getUuid().toString());
            return 1;
        }

        /// The player's username.
        /// @luaReturn string
        @LuaProperty
        public int getName(LuaState state) {
            state.pushString(player.getUsername());
            return 1;
        }

        /// The player's current position.
        /// @luaReturn vector
        @LuaProperty
        public int getPosition(LuaState state) {
            var pos = player.getPosition();
            state.pushVector((float) pos.x(), (float) pos.y(), (float) pos.z());
            return 1;
        }

        /// The player's yaw (horizontal rotation), in degrees.
        /// @luaReturn number
        @LuaProperty
        public int getYaw(LuaState state) {
            state.pushNumber(player.getPosition().yaw());
            return 1;
        }

        /// The player's pitch (vertical rotation), in degrees.
        /// @luaReturn number
        @LuaProperty
        public int getPitch(LuaState state) {
            state.pushNumber(player.getPosition().pitch());
            return 1;
        }

        /// The player's sidebar.
        /// @luaReturn @mapmaker/player.Sidebar
        @LuaProperty
        public int getSidebar(LuaState state) {
            throw new UnsupportedOperationException("TODO");
//            if (sidebar == null) {
//                sidebar = new Sidebar(this);
//                ScriptContext.track(state, sidebar);
//            }
//            LibPlayer$luau.pushSidebar(state, sidebar);
//            return 1;
        }

        /// The world as the player sees it. Use this to apply effects only this player can
        /// see, like ghost blocks or attached entities.
        /// @luaReturn @mapmaker/player.WorldView
        @LuaProperty
        public int getWorld(LuaState state) {
            LibPlayer$luau.pushWorldView(state, new WorldView(player));
            return 1;
        }

        /// **Deprecated.** The player's current scale.
        /// @luaReturn number
        @LuaProperty
        public int getDeprecated_scale(LuaState state) {
            state.pushNumber(player.getAttributeValue(Attribute.SCALE));
            return 1;
        }

        //endregion

        //region Events

        /// Fires when this player hits another player. Receives the player who was hit.
        /// @luaReturn @mapmaker.EventSource<@mapmaker/player.Player>
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

            LibBase.pushSignal(state, EntityAttackEvent.class, Impl::pushArgs);
            return 1;
        }

        /// Fires when this player right-clicks a block. Receives the block position.
        /// @luaReturn @mapmaker.EventSource<vector>
        @LuaProperty
        public int getOnInteractBlock(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, PlayerBlockInteractEvent event) {
                    if (event.getHand() != PlayerHand.MAIN) return -1; // ignore
                    LuaVector.push(state, event.getBlockPosition());
                    return 1;
                }
            }

            LibBase.pushSignal(state, PlayerBlockInteractEvent.class, Impl::pushArgs);
            return 1;
        }

        /// Fires when this player jumps.
        /// @luaReturn @mapmaker.EventSource<()>
        @LuaProperty
        public int getOnJump(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, PlayerJumpEvent event) {
                    return 0;
                }
            }

            LibBase.pushSignal(state, PlayerJumpEvent.class, Impl::pushArgs);
            return 1;
        }

        /// Fires when this player right-clicks while holding an item.
        /// @luaReturn @mapmaker.EventSource<()>
        @LuaProperty
        public int getOnUseItem(LuaState state) {
            class Impl {
                static int pushArgs(LuaState state, PlayerUseItemEvent event) {
                    if (event.getHand() != PlayerHand.MAIN) return -1; // Ignore
                    return 0;
                }
            }

            LibBase.pushSignal(state, PlayerUseItemEvent.class, Impl::pushArgs);
            return 1;
        }

        //endregion

        //region Instance Methods

        /// Sends a chat message to this player.
        ///
        /// @luaParam message AnyText
        @LuaMethod
        public void sendMessage(LuaState state) {
            var msg = LuaText.checkAnyText(state, 1);
            player.sendMessage(msg);
        }

        /// Shows a title (and optional subtitle) on this player's screen.
        ///
        /// @luaParam title AnyText
        /// @luaParam subtitle AnyText?
        // (self, title: AnyText, subtitle?: AnyText, { fadeIn?: number, stay?: number, fadeOut?: number }?)
        @LuaMethod
        public void showTitle(LuaState state) {
            int top = state.top();
            var title = LuaText.checkAnyText(state, 1);
            var subtitle = top > 1 ? LuaText.checkAnyText(state, 2) : Component.empty();
            // TODO: fade options

            player.showTitle(Title.title(title, subtitle));
        }

        /// Plays a sound for this player. The sound follows the player as they move.
        ///
        /// `options` can include `volume` (0–1), `pitch` (0–2), and `category`.
        ///
        /// @luaParam sound string
        /// @luaParam options { volume: number?, pitch: number?, category: string? }?
        @LuaMethod
        public void playSound(LuaState state) {
            var sound = Sound.sound();
            sound.type(checkKey(state, 1));

            readSoundOptions(state, sound, 2);

            player.playSound(sound.build(), Sound.Emitter.self());
        }

        /// Plays a sound for this player at a specific position. Volume falls off with distance.
        ///
        /// @luaParam sound string
        /// @luaParam position vector
        /// @luaParam options { volume: number?, pitch: number?, category: string? }?
        @LuaMethod
        public void playSoundAt(LuaState state) {
            var sound = Sound.sound();
            sound.type(checkKey(state, 1));
            var pos = LuaVector.check(state, 2);
            readSoundOptions(state, sound, 3);

            player.playSound(sound.build(), pos);
        }

        /// Stops sounds playing for this player. With no arguments, stops every sound.
        /// Pass `sound` to stop a specific key, `category` to stop a category, or both.
        ///
        /// @luaParam sound string?
        /// @luaParam category string?
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

        /// Teleports the player. Yaw and pitch default to the player's current values.
        ///
        /// `relative` is a string containing any of the characters `xyzrw` — each one means
        /// that axis is interpreted as an offset from the player's current value rather than
        /// an absolute coordinate. `r` covers yaw, `w` covers pitch.
        ///
        /// @luaParam position vector
        /// @luaParam yaw number?
        /// @luaParam pitch number?
        /// @luaParam relative string?
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

        /// Returns the item equipped in `slot`, or an empty item if the slot is empty.
        ///
        /// @luaParam slot string
        /// @luaReturn @mapmaker/item.Item
        @LuaMethod
        public int getSlot(LuaState state) {
            var slot = LibItem.checkSlot(state, 1);
            LibItem.pushItem(state, player.getEquipment(slot));
            return 1;
        }

        /// Sets the item equipped in `slot`.
        ///
        /// @luaParam slot string
        /// @luaParam item @mapmaker/item.Item
        @LuaMethod
        public void setSlot(LuaState state) {
            var slot = LibItem.checkSlot(state, 1);
            var item = LibItem.checkItemArg(state, 2);
            player.setEquipment(slot, item);
        }

        /// Places an item in the player's inventory. Slot indices: hotbar 0–8, main inventory
        /// 9–35, armor 36–39, off-hand 40.
        ///
        /// @luaParam slot number
        /// @luaParam item @mapmaker/item.Item
        @LuaMethod
        public void setItem(LuaState state) {
            var slot = state.checkInteger(1);
            var item = LibItem.checkItemArg(state, 2);
            player.getInventory().setItemStack(slot, item);
        }

        /// Applies a potion effect to this player. `amplifier` is 1-based — pass `1` for
        /// level I. Duration is in ticks; omit it for an infinite effect.
        ///
        /// @luaParam effect string
        /// @luaParam amplifier number?
        /// @luaParam duration number?
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

        /// Removes an active potion effect from this player.
        ///
        /// @luaParam effect string
        @LuaMethod
        public void removeEffect(LuaState state) {
            var effect = PotionEffect.fromKey(state.checkString(1));
            if (effect == null) throw state.error("unknown potion effect: " + state.toString(1));

            player.removeEffect(effect);
        }

        /// Removes all active potion effects from this player.
        @LuaMethod
        public void clearEffects(LuaState state) {
            player.clearEffects();
        }


        //endregion

        //region Meta Methods

        /// Two `Player` values are equal when they refer to the same player.
        @LuaMethod(meta = "__eq")
        public int luaEq(LuaState state) {
            var result = state.isUserData(1)
                         && state.toUserData(1) instanceof Player p
                         && p.player.getUuid().equals(player.getUuid());
            state.pushBoolean(result);
            return 1;
        }

        //endregion
    }

    /// The world as seen by a single player. Effects applied here are visible only to that
    /// player.
    @LuaExport
    public static final class WorldView {
        private final MapPlayer player;

        WorldView(MapPlayer player) {
            this.player = player;
        }

        /// Spawns an entity that only this player can see. `init` describes the entity —
        /// `position` is required, plus any of the entity's properties.
        ///
        /// Currently only `"text"` (a text display) is supported.
        ///
        /// ```luau
        /// player.world:spawn_entity("text", {
        ///     position = vector(0, 64, 0),
        ///     text = "<red>only you can see me</red>",
        /// })
        /// ```
        ///
        /// @luaParam entityType string
        /// @luaParam init $Writable<@mapmaker/entity.TextProp> & { position: vector, yaw: number?, pitch: number? }
        /// @luaReturn @mapmaker/entity.TextProp
        @LuaMethod
        public int spawnEntity(LuaState state) {
            throw new UnsupportedOperationException("TODO");
//            var typeName = state.checkString(1); // entity type
//            state.checkType(2, LuaType.TABLE); // init
//
//            if (!typeName.equals("text"))
//                throw state.error("Only text entity is supported");
//
//            var entity = new DisplayEntity.Text(UUID.randomUUID());
//            entity.setAutoViewable(false);
////            entity.updateViewerRule(other -> other == player);
//
//            var luaEntity = new LibEntity.TextDisplay(entity);
//            LuaHelpers.tableForEach(state, 2, (key) -> {
//                if ("position".equals(key) || "yaw".equals(key) || "pitch".equals(key))
//                    return; // Special handling below
//                if (!luaEntity.readField(state, key, -1)) {
//                    state.argError(2, "Unknown property: " + key);
//                }
//            });
//
//            if (!tableGet(state, 2, "position"))
//                state.argError(2, "Missing position");
//            Point point = LuaVector.check(state, -1);
//            state.pop(1); // remove position
//            float yaw = 0, pitch = 0;
//            if (tableGet(state, 2, "yaw")) {
//                yaw = (float) state.toNumber(-1);
//                state.pop(1); // remove yaw
//            }
//            if (tableGet(state, 2, "pitch")) {
//                pitch = (float) state.toNumber(-1);
//                state.pop(1); // remove position
//            }
//
//            entity.setInstance(player.getInstance(), new Pos(point, yaw, pitch));
//            entity.addViewer(player);
//            LegacyScriptContext.get(state).track(new Disposable() {
//                @Override
//                public void dispose() {
//                    entity.remove();
//                }
//
//                // TODO disposable might self dispose.
////                @Override
////                public boolean isDisposed() {
////                    return entity.isRemoved();
////                }
//            });
//
//            LibEntity.pushEntity(state, luaEntity);
//            return 1;
        }

        /// Sets a block visible only to this player. The actual world is unchanged.
        ///
        /// `blockState` accepts a block id with optional state, e.g. `"minecraft:stone"`
        /// or `"minecraft:oak_stairs[facing=north]"`.
        ///
        /// @luaParam position vector
        /// @luaParam blockState string
        @LuaMethod
        public void setBlock(LuaState state) {
            var position = LuaVector.check(state, 1);
            var blockState = state.checkString(2);
            var block = Block.fromState(blockState);

            GhostBlockHolder.forPlayer(player).setBlock(position, block);
        }

    }

//    /// A scoreboard sidebar shown on the right side of the player's screen.
//    @LuaExport
//    public static final class Sidebar implements Disposable {
//        private final Player player;
//        private final net.minestom.server.scoreboard.Sidebar delegate;
//        private boolean disposed = false;
//
//        private @Nullable LuaCallback cb;
//        private @Nullable ScheduledCallback scheduled;
//
//        public Sidebar(Player player) {
//            this.player = player;
//            delegate = new net.minestom.server.scoreboard.Sidebar(Component.empty());
//            delegate.addViewer(player.player);
//        }
//
//        /// Sets the sidebar's contents. The render function is called each tick and must
//        /// return a table containing `title` and an array of `lines`.
//        ///
//        /// ```luau
//        /// player.sidebar:set(function(p)
//        ///     return {
//        ///         title = "<gold>Stats</gold>",
//        ///         lines = { "Name: " .. p.name },
//        ///     }
//        /// end)
//        /// ```
//        ///
//        /// @luaParam render (player: @mapmaker/player.Player) -> { title: AnyText, lines: { AnyText } }
//        @LuaMethod
//        public void set(LuaState state) {
//            state.checkType(1, LuaType.FUNCTION);
//
//            // Replacing an existing render: tear down the old callback + task.
//            if (scheduled != null) scheduled.dispose();
//
//            var cb = LuaCallback.of(state, 1);
//            this.cb = cb;
//            this.scheduled = ScheduledCallback.recurring(state, cb, () -> {
//                var s = cb.state();
//                LibPlayer$luau.pushPlayer(s, player); // the single render arg
//                cb.call(1, 1);                        // -> [result table]
//
//                s.checkType(-1, LuaType.TABLE);
//                if (tableGet(s, -1, "title")) {
//                    var title = LuaText.checkAnyText(s, -1);
//                    s.pop(1);
//                    delegate.setTitle(title);
//                }
//
//                for (var line : Set.copyOf(delegate.getLines())) {
//                    delegate.removeLine(line.getId());
//                }
//                if (tableGet(s, -1, "lines")) {
//                    s.checkType(-1, LuaType.TABLE);
//                    arrayForEach(s, -1, (index) -> {
//                        var line = LuaText.checkAnyText(s, -1);
//                        delegate.createLine(new net.minestom.server.scoreboard.Sidebar.ScoreboardLine(
//                            String.valueOf(index), line, index, net.minestom.server.scoreboard.Sidebar.NumberFormat.blank()));
//                    });
//                    s.pop(1);
//                }
//
//                s.pop(1);
//            });
//        }
//
//        @Override
//        public void dispose() {
//            if (disposed) return;
//            disposed = true;
//            delegate.removeViewer(player.player);
//            if (scheduled != null) scheduled.dispose(); // cancels task + disposes cb
//        }
//    }

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
