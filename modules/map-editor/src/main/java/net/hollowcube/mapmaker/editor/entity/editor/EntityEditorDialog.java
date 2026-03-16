package net.hollowcube.mapmaker.editor.entity.editor;

import net.hollowcube.common.dialogs.DialogBuilder;
import net.hollowcube.common.dialogs.DialogButtons;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoRegistry;
import net.hollowcube.mapmaker.map.entity.info.MapEntityInfoType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerPickEntityEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EntityEditorDialog {

    private static final Key PROPERTIES_ID = Key.key("mapmaker:entity_editor/properties");

    private static final Key DELETE_BUTTON_ID = Key.key("mapmaker:entity_editor/button/delete");
    private static final Key SPAWN_EGG_BUTTON_ID = Key.key("mapmaker:entity_editor/button/spawn_egg");
    private static final Key FACE_ME_BUTTON_ID = Key.key("mapmaker:entity_editor/button/face_me");
    private static final Key PROPERTIES_BUTTON_ID = Key.key("mapmaker:entity_editor/button/properties");

    private static final String ENTITY_KEY = "mapmaker:entity";

    public static void handleDialogSubmit(PlayerCustomClickEvent event) {
        var player = event.getPlayer();
        var world = EditorMapWorld.forPlayer(player);
        if (world == null || !(event.getPayload() instanceof CompoundBinaryTag data)) return;
        if (!data.contains(ENTITY_KEY)) return;
        if (!world.canEdit(player)) return;

        var entityId = UUID.fromString(data.getString(ENTITY_KEY));
        if (!(world.instance().getEntityByUuid(entityId) instanceof MapEntity<?> entity)) return;

        if (event.getKey().equals(PROPERTIES_ID)) {
            handlePropertiesDialog(data, entity);
        } else if (event.getKey().equals(PROPERTIES_BUTTON_ID)) {
            var dialog = getPropertiesDialog(entity);
            if (dialog != null) {
                player.showDialog(dialog);
            } else {
                player.closeDialog();
            }
        } else if (event.getKey().equals(DELETE_BUTTON_ID)) {
            entity.remove();
            player.closeDialog();
        } else if (event.getKey().equals(SPAWN_EGG_BUTTON_ID)) {
            EntityEditor.handlePickEntity(new PlayerPickEntityEvent(player, entity, true));
            player.closeDialog();
        } else if (event.getKey().equals(FACE_ME_BUTTON_ID)) {
            entity.lookAt(player);
            player.closeDialog();
        }
    }

    public static @Nullable Dialog get(MapEntity<?> entity) {
        var options = MapEntityInfoRegistry.get(entity);
        if (options == null) return null;

        var data = getExtraData(entity);

        return DialogBuilder.create().closeOnEscape().buildActions(
            List.of(
                DialogButtons.button(Component.text("Edit Properties"), 150, PROPERTIES_BUTTON_ID, data),
                DialogButtons.button(Component.text("Delete Entity"), 150, DELETE_BUTTON_ID, data),
                DialogButtons.button(Component.text("Give Spawn Egg"), 150, SPAWN_EGG_BUTTON_ID, data),
                DialogButtons.button(Component.text("Look At Me"), 150, FACE_ME_BUTTON_ID, data)
            ),
            2
        );
    }

    private static @Nullable Dialog getPropertiesDialog(MapEntity<?> entity) {
        var options = MapEntityInfoRegistry.get(entity);
        if (options == null) return null;

        return DialogBuilder.create()
            .closeOnEscape()
            .inputs(inputs -> {
                int index = 0;
                for (var property : options.properties()) {
                    var input = MapEntityInfoType.castToInput(entity, property.type(), Integer.toString(index), property.name());
                    if (input != null) {
                        inputs.input(input);
                    }
                    index++;
                }
            })
            .buildSaveConfirmation(PROPERTIES_ID, getExtraData(entity));
    }

    private static void handlePropertiesDialog(CompoundBinaryTag data, MapEntity<?> entity) {
        var options = MapEntityInfoRegistry.get(entity);
        if (options == null) return;

        int index = 0;
        for (var property : options.properties()) {
            var option = data.get(Integer.toString(index));
            if (option != null) {
                MapEntityInfoType.castFromInput(entity, property.type(), option);
            }
            index++;
        }
    }

    private static CompoundBinaryTag getExtraData(MapEntity<?> entity) {
        return CompoundBinaryTag.builder()
            .put(ENTITY_KEY, StringBinaryTag.stringBinaryTag(entity.getUuid().toString()))
            .build();
    }
}
