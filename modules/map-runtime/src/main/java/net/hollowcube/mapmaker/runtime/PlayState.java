package net.hollowcube.mapmaker.runtime;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public final class PlayState {
    public sealed interface Attachment<T> extends Keyed permits AttachmentImpl {

        T copyValue(T value);

    }

    public static <T> Attachment<T> attachment(Key key, Codec<T> codec) {
        return new AttachmentImpl<>(key, codec, Function.identity());
    }

    public static <T> Attachment<T> attachment(Key key, Codec<T> codec, Function<T, T> copyFunc) {
        return new AttachmentImpl<>(key, codec, copyFunc);
    }

    private static final StructCodec<Map<Long, Block>> GHOST_BLOCKS_CODEC = StructCodec.struct(
            "ghostBlocks", ExtraCodecs.LONG_STRING.mapValue(ExtraCodecs.BLOCK_STATE_STRING).optional(Map.of()), Function.identity(),
            it -> it);
    private static final StructCodec<Map<String, Integer>> COOLDOWN_GROUPS_CODEC = StructCodec.struct(
            "cooldownGroups", Codec.STRING.mapValue(Codec.INT).optional(Map.of()), Function.identity(),
            it -> it);
    public static Codec<PlayState> CODEC = net.minestom.server.codec.Codec.Recursive(codec -> StructCodec.struct(
            "lastState", codec.optional(), PlayState::lastState,
            "history", net.minestom.server.codec.Codec.STRING.list().optional(List.of()), PlayState::history,
            "pos", ExtraCodecs.POS.optional(), PlayState::pos,
            StructCodec.INLINE, GHOST_BLOCKS_CODEC.optional(), PlayState::ghostBlocks,
            StructCodec.INLINE, COOLDOWN_GROUPS_CODEC.optional(), PlayState::cooldownGroups,
            StructCodec.INLINE, new ActionDataCodec(Integer.MAX_VALUE), PlayState::actionData,
            PlayState::new));

    public static final SaveStateType.Serializer<PlayState> SERIALIZER = SaveStateType.serializer("playState", CODEC, HCDataTypes.PLAY_STATE);

    private @Nullable PlayState lastState; // The previous state of the player (ie at the last checkpoint)
    // Has two meanings:
    // in main state -> The list of status reached since the last checkpoint.
    // in last state -> The last entry is always the checkpoint that was reached at that state.
    private final List<String> history;
    private @Nullable Pos pos;
    private Map<Long, Block> ghostBlocks;
    private Map<String, Integer> cooldownGroups;
    private Map<Attachment<?>, Object> actionData;

    public PlayState() {
        this(null, null, null, null, null, null);
    }

    public PlayState(
            @Nullable PlayState lastState, @Nullable List<String> statusEffects,
            @Nullable Pos pos, @Nullable Map<Long, Block> ghostBlocks,
            @Nullable Map<String, Integer> cooldownGroups,
            @Nullable Map<Attachment<?>, Object> actionData
    ) {
        this.lastState = lastState;
        this.history = new ArrayList<>(Objects.requireNonNullElse(statusEffects, List.of()));
        this.pos = pos;
        this.ghostBlocks = new HashMap<>(Objects.requireNonNullElse(ghostBlocks, Map.of()));
        this.cooldownGroups = new HashMap<>(Objects.requireNonNullElse(cooldownGroups, Map.of()));
        this.actionData = new HashMap<>(Objects.requireNonNullElse(actionData, Map.of()));
    }

    public boolean isEmpty() {
        return history.isEmpty() && pos == null && ghostBlocks.isEmpty() && actionData.isEmpty();
    }

    public @Nullable PlayState lastState() {
        return lastState;
    }

    public boolean hasStatus(String id) {
        return history.contains(id);
    }

    public List<String> history() {
        return history;
    }

    public Map<Attachment<?>, Object> actionData() {
        return actionData;
    }

    public <T> @Nullable T get(Attachment<T> attachment) {
        return (T) actionData.get(attachment);
    }

    public <T> T get(Attachment<T> attachment, T defaultValue) {
        return Objects.requireNonNullElse(get(attachment), defaultValue);
    }

    public <T> void set(Attachment<T> attachment, @Nullable T value) {
        if (value == null) {
            actionData.remove(attachment);
        } else {
            actionData.put(attachment, value);
        }
    }

    public <T> void update(Attachment<T> attachment, Function<T, T> updater) {
        var existing = get(attachment);
        if (existing != null) {
            actionData.put(attachment, updater.apply(existing));
        }
    }

    public @Nullable Pos pos() {
        return pos;
    }

    public Map<Long, Block> ghostBlocks() {
        return ghostBlocks;
    }

    public void setGhostBlocks(Map<Long, Block> blockMap) {
        this.ghostBlocks = new HashMap<>(blockMap);
    }

    public Map<String, Integer> cooldownGroups() {
        return cooldownGroups;
    }

    public void setCooldownGroups(Map<String, Integer> cooldownGroups) {
        this.cooldownGroups = new HashMap<>(cooldownGroups);
    }

    public void setLastState(@Nullable PlayState lastState) {
        this.lastState = lastState == null ? null : lastState.copy();
    }

    public void addStatus(String id) {
        if (!history.contains(id)) history.add(id);
    }

    public void clearStatus() {
        history.clear();
    }

    public void setPos(@Nullable Pos pos) {
        this.pos = pos;
    }

    public PlayState copy() {
        var newActionData = new HashMap<Attachment<?>, Object>(actionData.size());
        for (var entry : actionData.entrySet()) {
            //noinspection unchecked
            newActionData.put(entry.getKey(), ((Attachment<Object>) entry.getKey()).copyValue(entry.getValue()));
        }
        return new PlayState(lastState, history, pos, new HashMap<>(ghostBlocks), new HashMap<>(cooldownGroups), newActionData);
    }

    public void setFrom(PlayState other) {
        this.lastState = OpUtils.map(other.lastState, PlayState::copy);
        this.history.clear();
        this.history.addAll(other.history);
        this.pos = other.pos;
        this.ghostBlocks = new HashMap<>(other.ghostBlocks);
        this.actionData = new HashMap<>();
        for (var entry : other.actionData.entrySet()) {
            //noinspection unchecked
            this.actionData.put(entry.getKey(), ((Attachment<Object>) entry.getKey()).copyValue(entry.getValue()));
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean includeLast) {
        return "PlayState{" +
                "lastState=" + String.valueOf(lastState != null) +
                ", statusEffects=" + (history.size() > 1 ? history.size() : history) +
                ", pos=" + pretty(pos) +
                '}';
    }

    private String pretty(Object optional) {
        return String.valueOf(optional);
    }

    private record AttachmentImpl<T>(Key key, Codec<T> codec,
                                     Function<T, T> copy) implements Attachment<T> {
        private static final Map<Key, AttachmentImpl<?>> REGISTRY = new HashMap<>();

        public AttachmentImpl {
            REGISTRY.put(key, this);
        }

        @Override
        public T copyValue(T value) {
            return copy.apply(value);
        }
    }

    private record ActionDataCodec(int maxSize) implements StructCodec<Map<Attachment<?>, Object>> {
        private static final Codec<AttachmentImpl<?>> KEY_CODEC = Codec.KEY.transform(AttachmentImpl.REGISTRY::get, Keyed::key);

        @Override
        public <D> Result<Map<Attachment<?>, Object>> decodeFromMap(Transcoder<D> coder, Transcoder.MapLike<D> map) {
            if (map.size() > maxSize)
                return new Result.Error<>("Map size exceeds maximum allowed size: " + maxSize);
            if (map.isEmpty()) return new Result.Ok<>(Map.of());

            final Map<Attachment<?>, Object> decodedMap = new HashMap<>(map.size());
            for (final String key : map.keys()) {
                try {
                    Key.key(key);
                } catch (InvalidKeyException ignored) {
                    continue;
                }
                final Result<AttachmentImpl<?>> keyResult = KEY_CODEC.decode(coder, coder.createString(key));
                if (!(keyResult instanceof Result.Ok(AttachmentImpl<?> decodedKey)))
                    return keyResult.cast();
                if (decodedKey == null) continue;
                // The throwing decode here is fine since we are already iterating over known keys.
                final Result<?> valueResult = decodedKey.codec.decode(coder, map.getValue(key).orElseThrow());
                if (!(valueResult instanceof Result.Ok(Object decodedValue)))
                    return valueResult.cast();
                decodedMap.put(decodedKey, decodedValue);
            }
            return new Result.Ok<>(Map.copyOf(decodedMap));
        }

        @Override
        public <D> Result<D> encodeToMap(Transcoder<D> coder, Map<Attachment<?>, Object> value, Transcoder.MapBuilder<D> map) {
            if (value.size() > maxSize)
                return new Result.Error<>("Map size exceeds maximum allowed size: " + maxSize);
            if (value.isEmpty()) return new Result.Ok<>(coder.createMap().build());

            for (final Map.Entry<Attachment<?>, Object> entry : value.entrySet()) {
                final AttachmentImpl<Object> attachment = (AttachmentImpl<Object>) entry.getKey();
                final Result<D> keyResult = KEY_CODEC.encode(coder, attachment);
                if (!(keyResult instanceof Result.Ok(D encodedKey)))
                    return keyResult.cast();
                final Result<D> valueResult = attachment.codec.encode(coder, entry.getValue());
                if (!(valueResult instanceof Result.Ok(D encodedValue)))
                    return valueResult.cast();
                map.put(encodedKey, encodedValue);
            }

            return new Result.Ok<>(map.build());
        }
    }
}
