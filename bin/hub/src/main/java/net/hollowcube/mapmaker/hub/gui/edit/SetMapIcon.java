package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class SetMapIcon extends View {

    private static final Predicate<@Nullable Material> SEARCH_PREDICATE = material ->
            material != null &&
                    material != Material.AIR &&
                    material != Material.SCULK_SENSOR &&
                    material != Material.CALIBRATED_SCULK_SENSOR &&
                    material != Material.RECOVERY_COMPASS &&
                    !material.name().endsWith("glass_pane");

    public static final String SIG_UPDATE_ICON = "set_map_icon.selected";

    private @Outlet("input") Label inputField;
    private @Outlet("page") Pagination pagination;

    private String input = "";

    public SetMapIcon(@NotNull Context context) {
        super(context);
        inputField.setArgs(Component.text(input));
    }

    private Task task = null;

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        if (this.input.equals(input)) return;

        if (task != null) task.cancel();
        task = MinecraftServer.getSchedulerManager().buildTask(() -> {
            inputField.setArgs(Component.text(input));
            pagination.reset();
        }).delay(500, TimeUnit.MILLISECOND).schedule();

        this.input = input;
    }

    @Action("input")
    public void handleBackButton() {
        popView();
    }

    @Action("page")
    private void createPage(@NotNull Pagination.PageRequest<MapIconPreview> request) {
        List<MapIconPreview> result;
        if (input.isEmpty()) {
            // Add some random items
            result = ThreadLocalRandom.current().ints(1, Material.values().size())
                    .mapToObj(Material::fromId)
                    .filter(SEARCH_PREDICATE)
                    .limit(request.pageSize())
                    .map(m -> new MapIconPreview(request.context(), m))
                    .toList();
        } else {
            result = new ArrayList<>();
            for (var suggestion : Autocompletors.searchMaterials(input, request.pageSize(), SEARCH_PREDICATE)) {
                result.add(new MapIconPreview(request.context(), suggestion));
            }

            // Show a "no results" button if there are no results
            if (result.isEmpty()) {
                result.add(new MapIconPreview(request.context()));
            }
        }
        request.respond(result, false);
    }

    @Signal(MapIconPreview.SIG_SELECTED)
    private void handleSelectIcon(@NotNull Material material) {
        popView(SIG_UPDATE_ICON, material);
    }

    @Signal(SIG_CLOSE)
    private void handleClose() {
        var task = this.task;
        this.task = null;
        if (task != null) task.cancel();
    }

}
