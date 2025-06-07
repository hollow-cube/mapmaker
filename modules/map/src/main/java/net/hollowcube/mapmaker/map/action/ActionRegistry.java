package net.hollowcube.mapmaker.map.action;

import net.hollowcube.mapmaker.map.action.impl.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public final class ActionRegistry {
    private static final DynamicRegistry<StructCodec<? extends Action>> REGISTRY = DynamicRegistry.create(Key.key("mapmaker:action"));
    private static final DynamicRegistry<Action.Editor<? extends Action>> EDITOR_REGISTRY = DynamicRegistry.create(Key.key("mapmaker:action_editor"));
    private static final List<Key> KEYS = new ArrayList<>();

    public static final Codec<RegistryKey<StructCodec<? extends Action>>> KEY_CODEC = RegistryKey.codec(_ -> REGISTRY);
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

    static {
        register(EditLivesAction.KEY, EditLivesAction.CODEC, EditLivesAction.EDITOR);
        register(EditTimerAction.KEY, EditTimerAction.CODEC, EditTimerAction.EDITOR);
        register(AddPotionAction.KEY, AddPotionAction.CODEC, AddPotionAction.EDITOR);
        register(RemovePotionAction.KEY, RemovePotionAction.CODEC, RemovePotionAction.EDITOR);
        register(TeleportAction.KEY, TeleportAction.CODEC, TeleportAction.EDITOR);
        register(EnableSettingAction.KEY, EnableSettingAction.CODEC, EnableSettingAction.EDITOR);
        register(DisableSettingAction.KEY, DisableSettingAction.CODEC, DisableSettingAction.EDITOR);
        register(SetProgressIndexAction.KEY, SetProgressIndexAction.CODEC, SetProgressIndexAction.EDITOR);
        register(ResetHeightAction.KEY, ResetHeightAction.CODEC, ResetHeightAction.EDITOR);
        register(GiveElytraAction.KEY, GiveElytraAction.CODEC, GiveElytraAction.EDITOR);
        register(TakeElytraAction.KEY, TakeElytraAction.CODEC, TakeElytraAction.EDITOR);
        register(GiveItemAction.KEY, GiveItemAction.CODEC, GiveItemAction.EDITOR);
        register(TakeItemAction.KEY, TakeItemAction.CODEC, TakeItemAction.EDITOR);
    }

    private static <T extends Action> RegistryKey<StructCodec<? extends Action>> register(Key name, StructCodec<T> codec, Action.Editor<T> editor) {
        var key = REGISTRY.register(name, codec);
        EDITOR_REGISTRY.register(name, editor);
        KEYS.add(name);
        return key;
    }

}
