package net.hollowcube.mapmaker.map.gui.displayentity;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ActionGroup;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.common.ConfirmAction;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.feature.edit.DisplayEntityEditingFeatureProvider;
import net.hollowcube.terraform.entity.TerraformEntity;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AbstractEditDisplayView<E extends DisplayEntity, M extends AbstractDisplayMeta> extends View {

    private @Outlet("tab_properties_switch") Switch tabPropertiesSwitch;
    private @Outlet("tab_switch_middle_slot") Switch tabSwitchMiddleSlot;
    private @Outlet("tab_transform_switch") Switch tabTransformSwitch;

    private @Outlet("option_billboard") Switch billboardOption;

    private @Outlet("page") Switch page;
    private @Outlet("tab_text") Text title;

    protected final E display;

    public AbstractEditDisplayView(@NotNull Context context, E display) {
        super(context.with(Map.of("display", display)));
        this.display = display;

        this.setPage(0);
    }

    protected M meta() {
        //noinspection unchecked
        return (M) this.display.getEntityMeta();
    }

    @MustBeInvokedByOverriders
    protected void updateState() {
        var meta = this.display.getEntityMeta();
        this.billboardOption.setOption(meta.getBillboardRenderConstraints().ordinal());
    }

    @ActionGroup("option_billboard_.*")
    private void onCycleBillboard() {
        var meta = this.display.getEntityMeta();
        var values = AbstractDisplayMeta.BillboardConstraints.values();
        meta.setBillboardRenderConstraints(values[(meta.getBillboardRenderConstraints().ordinal() + 1) % values.length]);
        this.updateState();
    }

    @ActionGroup("tab_properties_off|tab_middle_slot_properties")
    private void openPropertiesTab() {
        this.setPage(0);
    }

    @ActionGroup("tab_transform_off|tab_middle_slot_transform")
    private void openTransformTab() {
        this.setPage(1);
    }

    @Action("duplicate")
    private void onDuplicateClick(@NotNull Player player) {
        var data = TerraformEntity.writeToTagWithPassengers(this.display).remove("uuid");
        TerraformEntity.spawnWithPassengers(player, this.display.getInstance(), data);
        player.closeInventory();
    }

    @Action("deselect")
    private void onDeselectClick(@NotNull Player player) {
        DisplayEntityEditingFeatureProvider.setSelectedDisplayEntity(player, null);
        player.closeInventory();
    }

    @Action("teleport")
    private void onTeleportClick(@NotNull Player player) {
        player.teleport(this.display.getPosition());
        player.closeInventory();
    }

    @Action("delete")
    private void onDuplicateDelete(@NotNull Player player) {
        pushView(context -> new ConfirmAction(
                context,
                () -> {
                    this.display.remove();
                    player.closeInventory();
                },
                Component.translatable("delete this display")
        ));
    }

    @Action("create")
    private void onCreateClick(@NotNull Player player) {
        replaceView(CreateDisplayView::new);
    }

    private void setPage(int page) {
        this.tabPropertiesSwitch.setOption(page ^ 1);
        this.tabSwitchMiddleSlot.setOption(page ^ 1);
        this.tabTransformSwitch.setOption(page);
        this.page.setOption(page);

        var title = switch (page) {
            case 0 -> Component.translatable("gui.display_entity.tab.properties");
            case 1 -> Component.translatable("gui.display_entity.tab.transform");
            default -> Component.empty();
        };

        this.title.setText(title);
        this.title.setArgs(title);
    }

    public static View create(@NotNull Context context, DisplayEntity entity) {
        return switch (entity) {
            case DisplayEntity.Block block -> new EditBlockDisplayView(context, block);
            case DisplayEntity.Item item -> new EditItemDisplayView(context, item);
            case DisplayEntity.Text text -> new EditTextDisplayView(context, text);
        };
    }

}
