package net.hollowcube.mapmaker.map.entity.interaction;

import net.hollowcube.common.dialogs.DialogBuilder;
import net.hollowcube.common.math.MathUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.common.util.Uuids;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.notifications.ToastManager;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityEditor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class InteractionEditorScreen {

    private static final Key EDITOR_SCREEN_ID = Key.key("hollowcube", "interaction_editor_screen");
    private static final Pattern POSITION_REGEX = Pattern.compile(" *(?<x>-?\\d+(?:\\.\\d+)?) +(?<y>-?\\d+(?:\\.\\d+)?) +(?<z>-?\\d+(?:\\.\\d+)?) *");
    private static final TagStringIO TAG_STRING_IO = TagStringIO.builder()
        .acceptHeterogeneousLists(true)
        .emitHeterogeneousLists(true)
        .indent(4)
        .build();

    public static final ObjectEntityEditor MARKER_EDITOR = (player, entity) -> {
        if (entity instanceof InteractionEntity interaction) {
            InteractionEditorScreen.openEditorScreen(interaction, player);
            return true;
        }
        return false;
    };

    // region Data

    private static Pos getPosition(@NotNull String data, @NotNull InteractionEntity interaction) {
        var match = POSITION_REGEX.matcher(data);
        if (!match.matches()) return interaction.getPosition();
        return new Pos(
            MathUtil.parseFiniteDouble(match.group("x"), interaction.getPosition().x()),
            MathUtil.parseFiniteDouble(match.group("y"), interaction.getPosition().y()),
            MathUtil.parseFiniteDouble(match.group("z"), interaction.getPosition().z())
        );
    }

    private static BoundingBox getBoundingBox(@NotNull String width, @NotNull String height, @NotNull InteractionEntity interaction) {
        var bb = interaction.getBoundingBox();
        var w = MathUtil.parseFiniteDouble(width, bb.width());
        var h = MathUtil.parseFiniteDouble(height, bb.height());
        return new BoundingBox(w, h, w);
    }

    public static CompoundBinaryTag getData(@NotNull String data) {
        try {
            return TAG_STRING_IO.asCompound(data);
        } catch (Exception e) {
            return null;
        }
    }

    // endregion

    public static void onCallback(@NotNull PlayerCustomClickEvent event) {
        if (!event.getKey().equals(EDITOR_SCREEN_ID)) return;
        if (!(event.getPayload() instanceof CompoundBinaryTag payload)) return;
        var world = MapWorld.forPlayer(event.getPlayer());
        var uuid = Uuids.parse(payload.getString("uuid"));

        if (uuid == null || world == null || !world.canEdit(event.getPlayer())) return;

        var entity = world.instance().getEntityByUuid(uuid);
        if (!(entity instanceof InteractionEntity interaction)) return;

        var pos = getPosition(payload.getString("position"), interaction);
        var bb = getBoundingBox(payload.getString("width"), payload.getString("height"), interaction);
        var data = getData(payload.getString("data"));

        if (data != null) {
            interaction.setData(data, event.getPlayer(), false);
            event.getPlayer().closeDialog();
        } else {
            ToastManager.showNotification(
                event.getPlayer(),
                Component.text("Error"),
                Component.text("Can't parse, make sure it's valid NBT.", NamedTextColor.RED)
            );
        }
        interaction.teleport(pos);
        interaction.setBoundingBox(bb);
    }

    public static void openEditorScreen(@NotNull InteractionEntity entity, @NotNull Player player) {
        if (!ProtocolVersions.hasProtocolVersion(player, ProtocolVersions.V1_21_6)) {
            player.sendMessage(Component.translatable("dialog.interaction_entity.unsupported"));
        } else {
            try {
                var width = String.valueOf(entity.getEntityMeta().getWidth());
                var height = String.valueOf(entity.getEntityMeta().getHeight());
                var position = String.format("%.2f %.2f %.2f", entity.getPosition().x(), entity.getPosition().y(), entity.getPosition().z());
                var nbt = TAG_STRING_IO.asString(entity.getCleanData());


                var dialog = DialogBuilder.create()
                    .closeOnEscape()
                    .inputs(it -> it
                        .text("width", Component.text("Width"), width, 10, 250)
                        .text("height", Component.text("Height"), height, 10, 250)
                        .text("position", Component.text("Position (X Y Z)"), position, 50, 250)
                        .multiline("data", Component.text("Data"), nbt, -1, -1, 250, 150)
                    )
                    .buildSaveConfirmation(EDITOR_SCREEN_ID, CompoundBinaryTag.builder().putString("uuid", entity.getUuid().toString()).build());

                player.sendPacket(new ShowDialogPacket(dialog));
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
            }
        }
    }
}
