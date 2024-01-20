package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.BaseEffectData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractEffectActionsTab<EffectData extends BaseEffectData> extends View {
    private @Outlet("potion_effects") Label potionEffectsLabel;
    private @Outlet("clear_effects") Label clearEffectsLabel;
    private @Outlet("teleport") Label teleportLabel;
    private @Outlet("settings") Label settingsLabel;
    private @Outlet("add_item") Label addItemLabel;
    private @Outlet("remove_item") Label removeItemLabel;

    protected EffectData data;

    protected AbstractEffectActionsTab(@NotNull Context context) {
        super(context);
    }

    public void setData(@NotNull EffectData data) {
        this.data = data;

        updateFromData();
    }

    @Action("clear_effects")
    public void toggleClearEffects() {
        data.setClearPotionEffects(!data.clearPotionEffects());
        updateFromData();
    }

    @Action("teleport")
    public void handleTeleportInteract(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK) {
            if (data.teleport().isEmpty()) {
                // Default to the player's current position
                data.setTeleport(player.getPosition());
                updateFromData();
            }

            pushView(context -> new CoordinateEditorView(context, pos -> {
                data.setTeleport(pos);
                updateFromData();
            }, data.teleport().orElseThrow()));
        } else if (clickType == ClickType.RIGHT_CLICK) {
            data.setTeleport(null);
            updateFromData();
        }
    }


    protected void updateFromData() {
//        {
//            potionEffectsLabel.setComponentsDirect();
//        }

        clearEffectsLabel.setArgs(Component.translatable("gui.effect.actions.clear_effects." +
                (data.clearPotionEffects() ? "enabled" : "disabled")));

        if (data.teleport().isPresent()) {
            var teleTarget = data.teleport().get();
            teleportLabel.setArgs(Component.translatable("gui.effect.actions.teleport.pos", List.of(
                    Component.text(teleTarget.blockX()), Component.text(teleTarget.blockY()), Component.text(teleTarget.blockZ()),
                    Component.text(teleTarget.yaw()), Component.text(teleTarget.pitch())
            )));
        } else {
            teleportLabel.setArgs(Component.translatable("gui.effect.actions.teleport.none"));
        }

        //todo settings

        //todo items
    }
}
