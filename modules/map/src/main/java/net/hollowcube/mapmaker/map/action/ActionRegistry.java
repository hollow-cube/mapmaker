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
        register("mapmaker:lives", EditLivesAction.CODEC, EditLivesAction.EDITOR);
        register("mapmaker:timer", EditTimerAction.CODEC, EditTimerAction.EDITOR);
        register("mapmaker:add_potion", AddPotionAction.CODEC, AddPotionAction.EDITOR);
        register("mapmaker:remove_potion", RemovePotionAction.CODEC, RemovePotionAction.EDITOR);
        register("mapmaker:teleport", TeleportAction.CODEC, TeleportAction.EDITOR);
        register("mapmaker:enable_setting", EnableSettingAction.CODEC, EnableSettingAction.EDITOR);
        register("mapmaker:disable_setting", DisableSettingAction.CODEC, DisableSettingAction.EDITOR);
    }

    private static <T extends Action> void register(String name, StructCodec<T> codec, Action.Editor<T> editor) {
        REGISTRY.register(name, codec);
        EDITOR_REGISTRY.register(name, editor);
        KEYS.add(Key.key(name));
    }

}
