package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.mapmaker.runtime.parkour.action.impl.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.registry.RegistryKey;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public final class ActionRegistry {
    private static final DynamicRegistry<StructCodec<? extends Action>> REGISTRY = DynamicRegistry.create(Key.key("mapmaker:action"));
    private static final DynamicRegistry<Action.Editor<? extends Action>> EDITOR_REGISTRY = DynamicRegistry.create(Key.key("mapmaker:action_editor"));
    private static final Map<Action.Type, List<Key>> KEYS = new HashMap<>();

    public static final Codec<RegistryKey<StructCodec<? extends Action>>> KEY_CODEC = RegistryKey.codec(_ -> REGISTRY);
    public static final Codec<Action> CODEC = Codec.RegistryTaggedUnion(_ -> REGISTRY, Action::codec, "type");

    public static Codec<ActionList> listCodec(int maxSize) {
        return CODEC.list(maxSize).transform(ActionList::new, ActionList::actions).optional(new ActionList());
    }

    public static Key getKey(Action action) {
        var registryKey = REGISTRY.getKey(action.codec());
        return Objects.requireNonNull(registryKey).key();
    }

    public static Action.Editor<?> getEditor(Key key) {
        var editor = EDITOR_REGISTRY.get(key);
        if (editor == null)
            throw new IllegalArgumentException("No action editor found for key: " + key);
        return editor;
    }

    public static Action createDefault(Key key) {
        var codec = REGISTRY.get(key);
        if (codec == null)
            throw new IllegalArgumentException("No action found for key: " + key);
        return codec.decode(Transcoder.NBT, CompoundBinaryTag.empty()).orElseThrow();
    }

    public static List<Key> keys(Action.Type type) {
        return KEYS.getOrDefault(type, List.of());
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
        register(EditAttributeAction.KEY, EditAttributeAction.CODEC, EditAttributeAction.EDITOR);
        register(ChatAction.KEY, ChatAction.CODEC, ChatAction.EDITOR);
        register(RespawnPosAction.KEY, RespawnPosAction.CODEC, RespawnPosAction.EDITOR, Action.Type.CHECKPOINT);
        register(EditVariableAction.KEY, EditVariableAction.CODEC, EditVariableAction.EDITOR);
        register(EditVelocityAction.KEY, EditVelocityAction.CODEC, EditVelocityAction.EDITOR, Action.Type.STATUS);
    }

    private static <T extends Action> RegistryKey<StructCodec<? extends Action>> register(Key name, StructCodec<T> codec, Action.Editor<T> editor, Action.Type... types) {
        var key = REGISTRY.register(name, codec);
        EDITOR_REGISTRY.register(name, editor);
        if (types.length == 0) types = Action.Type.values();
        for (var type : types) {
            KEYS.computeIfAbsent(type, _ -> new ArrayList<>()).add(name);
        }
        return key;
    }

}
