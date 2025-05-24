package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.map.action.impl.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.DynamicRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class ActionRegistry {
    private static final DynamicRegistry<StructCodec<? extends Action>> REGISTRY = DynamicRegistry.create("mapmaker:action");
    private static final DynamicRegistry<Action.Editor<? extends Action>> EDITOR_REGISTRY = DynamicRegistry.create("mapmaker:action_editor");
    private static final List<Key> KEYS = new ArrayList<>();

    public static final Codec<DynamicRegistry.Key<StructCodec<? extends Action>>> KEY_CODEC = Codec.RegistryKey(_ -> REGISTRY);
    public static final Codec<Action> CODEC = Codec.RegistryTaggedUnion(_ -> REGISTRY, Action::codec, "type");

    public static @NotNull Codec<ActionList> listCodec(int maxSize) {
        return CODEC.list(maxSize).transform(ActionList::new, ActionList::actions).optional(new ActionList());
    }

    public static @NotNull Key getKey(@NotNull Action action) {
        var registryKey = REGISTRY.getKey(action.codec());
        return Objects.requireNonNull(registryKey).key();
    }

    public static @NotNull Action.Editor<?> getEditor(@NotNull Key key) {
        var editor = EDITOR_REGISTRY.get(key);
        if (editor == null)
            throw new IllegalArgumentException("No action editor found for key: " + key);
        return editor;
    }

    public static @NotNull Action createDefault(@NotNull Key key) {
        var codec = REGISTRY.get(key);
        if (codec == null)
            throw new IllegalArgumentException("No action found for key: " + key);
        return codec.decode(Transcoder.NBT, CompoundBinaryTag.empty()).orElseThrow();
    }

    public static @NotNull List<Key> keys() {
        return KEYS;
    }

    public static final DynamicRegistry.Key<StructCodec<? extends Action>> LIVES = register(EditLivesAction.KEY, EditLivesAction.CODEC, EditLivesAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> TIMER = register(EditTimerAction.KEY, EditTimerAction.CODEC, EditTimerAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> ADD_POTION = register(AddPotionAction.KEY, AddPotionAction.CODEC, AddPotionAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> REMOVE_POTION = register(RemovePotionAction.KEY, RemovePotionAction.CODEC, RemovePotionAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> TELEPORT = register(TeleportAction.KEY, TeleportAction.CODEC, TeleportAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> ENABLE_SETTING = register(EnableSettingAction.KEY, EnableSettingAction.CODEC, EnableSettingAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> DISABLE_SETTING = register(DisableSettingAction.KEY, DisableSettingAction.CODEC, DisableSettingAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> PROGRESS_INDEX = register(SetProgressIndexAction.KEY, SetProgressIndexAction.CODEC, SetProgressIndexAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> RESET_HEIGHT = register(ResetHeightAction.KEY, ResetHeightAction.CODEC, ResetHeightAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> GIVE_ELYTRA = register(GiveElytraAction.KEY, GiveElytraAction.CODEC, GiveElytraAction.EDITOR);
    public static final DynamicRegistry.Key<StructCodec<? extends Action>> TAKE_ELYTRA = register(TakeElytraAction.KEY, TakeElytraAction.CODEC, TakeElytraAction.EDITOR);

    private static <T extends Action> DynamicRegistry.Key<StructCodec<? extends Action>> register(Key name, StructCodec<T> codec, Action.Editor<T> editor) {
        var key = REGISTRY.register(name, codec);
        EDITOR_REGISTRY.register(name, editor);
        KEYS.add(name);
        return key;
    }

}
