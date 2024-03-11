package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.gui.effect.potion.PotionEffectListView;
import net.hollowcube.mapmaker.map.gui.effect.potion.PotionEffectSelectorView;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public abstract class AbstractEffectActionsTab<EffectData extends BaseEffectData> extends View {
    private static final Component TELEPORT_NONE = Component.translatable("gui.effect.actions.teleport.none");

    private @Outlet("potion_effects_switch") Switch potionEffectsSwitch;
    private @Outlet("potion_effects_off") Label potionEffectsOffLabel;
    private @Outlet("potion_effects_on") Label potionEffectsOnLabel;

    private @Outlet("clear_effects_switch") Switch clearEffectsSwitch;
    private @Outlet("clear_effects_off") Label clearEffectsOffLabel;
    private @Outlet("clear_effects_on") Label clearEffectsOnLabel;

    private @Outlet("teleport_switch") Switch teleportSwitch;
    private @Outlet("teleport_off") Label teleportOffLabel;
    private @Outlet("teleport_on") Label teleportOnLabel;

    private @Outlet("settings_switch") Switch settingsSwitch;
    private @Outlet("settings_off") Label settingsOffLabel;
    private @Outlet("settings_on") Label settingsOnLabel;

    private @Outlet("add_item_switch") Switch addItemSwitch;
    private @Outlet("add_item_off") Label addItemOffLabel;
    private @Outlet("add_item_on") Label addItemOnLabel;

    private @Outlet("remove_item_switch") Switch removeItemSwitch;
    private @Outlet("remove_item_off") Label removeItemOffLabel;
    private @Outlet("remove_item_on") Label removeItemOnLabel;

    protected EffectData data;
    protected Runnable save;

    protected AbstractEffectActionsTab(@NotNull Context context) {
        super(context);
    }

    public void setData(@NotNull EffectData data, @UnknownNullability Runnable save) {
        this.data = data;
        this.save = save;

        updateFromData();
    }

    @Action("potion_effects_off")
    public void addPotionEffectA() {
        // If this is the first effect go straight to the selector view
        // Otherwise open the list view
        if (data.potionEffects().isEmpty()) {
            pushTransientView(c -> new PotionEffectSelectorView(c, data.potionEffects(), save));
        } else {
            pushView(c -> new PotionEffectListView(c, data.potionEffects(), save));
        }
    }

    @Action("potion_effects_on")
    public void addPotionEffectB() {
        addPotionEffectA();
    }

    @Action("clear_effects_off")
    public void toggleClearEffectsA() {
        data.setClearPotionEffects(true);
        updateFromData();
    }

    @Action("clear_effects_on")
    public void toggleClearEffectsB() {
        data.setClearPotionEffects(false);
        updateFromData();
    }

    @Signal(Element.SIG_MOUNT)
    public void onMount() {
        updateFromData();
    }

    @Action("teleport_off")
    public void handleTeleportInteractA(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK || clickType == ClickType.RIGHT_CLICK) {
            if (data.teleport().isEmpty()) {
                // Default to the player's current position
                data.setTeleport(player.getPosition());
                updateFromData();
            }

            pushView(context -> new CoordinateEditorView(context, pos -> {
                data.setTeleport(pos);
                updateFromData();
            }, data.teleport().orElseThrow()));
        } else if (clickType == ClickType.START_SHIFT_CLICK) {
            data.setTeleport(null);
            updateFromData();
        }
    }

    @Action("teleport_on")
    public void handleTeleportInteractB(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        handleTeleportInteractA(player, slot, clickType);
    }

    protected void updateFromData() {
        potionEffectsSwitch.setOption(data.potionEffects().isEmpty() ? 0 : 1);

        var clearEffectsState = Component.translatable("gui.effect.actions.clear_effects." +
                (data.clearPotionEffects() ? "enabled" : "disabled"));
        clearEffectsOffLabel.setArgs(clearEffectsState);
        clearEffectsOnLabel.setArgs(clearEffectsState);
        clearEffectsSwitch.setOption(data.clearPotionEffects() ? 1 : 0);

        if (data.teleport().isPresent()) {
            teleportSwitch.setOption(1);
            var teleTarget = data.teleport().get();
            teleportOnLabel.setArgs(Component.translatable("gui.effect.actions.teleport.pos", List.of(
                    Component.text(teleTarget.blockX()), Component.text(teleTarget.blockY()), Component.text(teleTarget.blockZ()),
                    Component.text(teleTarget.yaw()), Component.text(teleTarget.pitch())
            )));
        } else {
            teleportSwitch.setOption(0);
            teleportOffLabel.setArgs(TELEPORT_NONE);
        }

        settingsSwitch.setOption(0);
        addItemSwitch.setOption(0);
        removeItemSwitch.setOption(0);
    }
}
