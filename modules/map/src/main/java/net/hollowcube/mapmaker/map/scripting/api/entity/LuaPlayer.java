package net.hollowcube.mapmaker.map.scripting.api.entity;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.block.ghost.GhostBlockHolder;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCheckpointChangeEvent;
import net.hollowcube.mapmaker.map.feature.play.BaseParkourMapFeatureProvider;
import net.hollowcube.mapmaker.map.scripting.api.LuaEventSource;
import net.hollowcube.mapmaker.map.scripting.api.math.LuaVectorTypeImpl;
import net.hollowcube.mapmaker.map.scripting.api.world.LuaBlock;
import net.hollowcube.mapmaker.map.scripting.api.world.LuaParticle;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static net.hollowcube.mapmaker.map.scripting.util.LuaHelpers.*;

// Note that Player is not an entity in Lua, though it does share some methods
public final class LuaPlayer {
    private static final String NAME = "Player";

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void init(@NotNull LuaState state) {
        // Create the metatable for Player
        state.newMetaTable(NAME);
        state.pushCFunction(LuaPlayer::luaIndex, "__index");
        state.setField(-2, "__index");
        state.pushCFunction(LuaPlayer::luaNameCall, "__namecall");
        state.setField(-2, "__namecall");
        state.pop(1);
    }

    public static void push(@NotNull LuaState state, @NotNull LuaPlayer player) {
        state.newUserData(player);
        state.getMetaTable(NAME);
        state.setMetaTable(-2);
    }

    public static @NotNull LuaPlayer checkArg(@NotNull LuaState state, int index) {
        return (LuaPlayer) state.checkUserDataArg(index, NAME);
    }

    private final Player delegate;

    public LuaPlayer(@NotNull Player delegate) {
        this.delegate = delegate;
    }

    // Properties

    private int getUuid(@NotNull LuaState state) {
        state.pushString(delegate.getUuid().toString());
        return 1;
    }

    private int getUsername(@NotNull LuaState state) {
        state.pushString(delegate.getUsername());
        return 1;
    }

    private int getPosition(@NotNull LuaState state) {
        LuaVectorTypeImpl.push(state, delegate.getPosition());
        return 1;
    }

    private int checkpointChanged(@NotNull LuaState state) {
        LuaEventSource.push(state, new LuaEventSource<>(delegate, MapPlayerCheckpointChangeEvent.class));
        return 1;
    }

    private int getProgressIndex(@NotNull LuaState state) {
        var saveState = SaveState.optionalFromPlayer(delegate);
        var playState = OpUtils.map(saveState, ss -> ss.state(PlayState.class));
        int progressIndex = OpUtils.mapOr(playState, ps -> ps.get(Attachments.PROGRESS_INDEX, 0), 0);
        state.pushInteger(progressIndex);
        return 1;
    }


    // Methods

    private int sendMessage(@NotNull LuaState state) {
        final String message = state.toString(1);
        delegate.sendMessage(MM.deserialize(message));
        return 0;
    }

    private int spawnParticle(@NotNull LuaState state) {
        final Particle particle = LuaParticle.checkArg(state, 1);
        final Point pos = LuaVectorTypeImpl.checkArg(state, 2);

        Point delta = Vec.ZERO;
        float speed = 0f;
        if (state.getTop() > 2) {
            delta = LuaVectorTypeImpl.checkArg(state, 3);
            speed = (float) state.checkNumberArg(4);
        }

        int count = 0; // Single particle
        if (state.getTop() > 4) {
            // TODO how should we actually limit particle sending?
            count = Math.clamp(state.checkIntegerArg(5), 0, 1000);
        }

        delegate.sendPacket(new ParticlePacket(particle, pos, delta, speed, count));
        return 0;
    }

    private int playSound(@NotNull LuaState state) {
        int index = 1, top = state.getTop();
        final Key soundKey = checkKeyArg(state, index++);
        final var soundEvent = OpUtils.or(SoundEvent.fromKey(soundKey),
                () -> SoundEvent.of(soundKey, null));

        Point pos = null;
        if (top >= index && state.type(index) == LuaType.VECTOR)
            pos = LuaVectorTypeImpl.checkArg(state, index++);

        float volume = 1.0f, pitch = 1.0f;
        if (top >= index) {
            volume = (float) state.checkNumberArg(index++);
            if (top >= index) {
                pitch = (float) state.checkNumberArg(index);
            }
        }

        // todo source enum
        Sound sound = Sound.sound(soundEvent, Sound.Source.MASTER, volume, pitch);
        if (pos != null) this.delegate.playSound(sound, pos);
        else this.delegate.playSound(sound);
        return 0;
    }

    private int setBlock(@NotNull LuaState state) {
        var blockPosition = LuaVectorTypeImpl.checkArg(state, 1);
        var block = LuaBlock.checkArg(state, 2);

        var ghostBlocks = GhostBlockHolder.forPlayer(delegate);
        ghostBlocks.setBlock(blockPosition, block);
        return 0;
    }

    /// SpawnEntity(type: EntityType, properties: table | nil): Entity
    /// Defaults to spawning at the player's feet with 0 yaw/pitch.
    private int spawnEntity(@NotNull LuaState state) {
        var entityType = checkKeyArg(state, 1);
        if (state.getTop() > 1 && state.type(2) != LuaType.TABLE) {
            state.argError(2, "Expected a table for properties");
            return 0; // Never reached, just to make java happy
        }

        var entityWrapper = LuaEntityCtor.create(entityType, null);
        if (entityWrapper == null) {
            state.error("Unknown entity type: " + entityType);
            return 0; // Never reached, just to make java happy
        }

        // Apply any properties from the given table, if present.
        Point position = delegate.getPosition();
        float yaw = 0f, pitch = 0f;
        if (state.getTop() > 1) {
            state.pushNil();
            while (state.next(2)) {
                // Key is at index -2, value is at index -1
                String key = state.toString(-2);
                switch (key) {
                    case "Position" -> position = LuaVectorTypeImpl.checkArg(state, -1);
                    case "Yaw" -> yaw = (float) state.checkNumberArg(-1);
                    case "Pitch" -> pitch = (float) state.checkNumberArg(-1);
                    default -> entityWrapper.readFieldFromTable(state, key);
                }

                // Remove the value, keep the key for the next iteration
                state.pop(1);
            }
        }

        // Spawn the entity
        var entity = entityWrapper.delegate();
        if (entity instanceof MapEntity mapEntity) {
            mapEntity.doNotSerialize();
        }

        // Track the entity in the player
        var entities = new ArrayList<>(delegate.getTag(BaseParkourMapFeatureProvider.OWNED_ENTITIES));
        entities.add(entity.getEntityId());
        delegate.setTag(BaseParkourMapFeatureProvider.OWNED_ENTITIES, entities);

        entity.setAutoViewable(false);
        entity.setInstance(delegate.getInstance(), new Pos(position, yaw, pitch))
                .thenRun(() -> entity.addViewer(delegate));

        LuaEntity.push(state, entityWrapper);
        return 1;
    }

    // Metamethods

    private static int luaIndex(@NotNull LuaState state) {
        final LuaPlayer player = checkArg(state, 1);
        final String key = state.checkStringArg(2);
        return switch (key) {
            case "Uuid" -> player.getUuid(state);
            case "Username" -> player.getUsername(state);
            case "Position" -> player.getPosition(state);
            // Events
            case "CheckpointChanged" -> player.checkpointChanged(state);
            // PK related, not sure if these should be here
            case "ProgressIndex" -> player.getProgressIndex(state);
            default -> noSuchKey(state, NAME, key);
        };
    }

    private static int luaNameCall(@NotNull LuaState state) {
        final LuaPlayer player = checkArg(state, 1);
        state.remove(1); // Remove the player userdata from the stack
        final String methodName = state.nameCallAtom();
        return switch (methodName) {
            case "SendMessage" -> player.sendMessage(state);
            case "SpawnParticle" -> player.spawnParticle(state);
            case "PlaySound" -> player.playSound(state);
            case "SetBlock" -> player.setBlock(state);
            case "SpawnEntity" -> player.spawnEntity(state);
            default -> noSuchMethod(state, NAME, methodName);
        };
    }

}
