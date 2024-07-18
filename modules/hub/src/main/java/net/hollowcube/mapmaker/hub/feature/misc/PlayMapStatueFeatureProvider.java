package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.gui.play.PlayMapsView;
import net.hollowcube.mapmaker.gui.play.QueryMapsView;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.color.Color;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@AutoService(HubFeature.class)
public class PlayMapStatueFeatureProvider implements HubFeature {

    private static final Pos LEFT = new Pos(-32.5, 38, 15.5);
    private static final Pos LEFT_MIDDLE = new Pos(-35.5, 38, 7.5);
    private static final Pos MIDDLE = new Pos(-36.5, 37, 0.5);
    private static final Pos RIGHT_MIDDLE = new Pos(-35.5, 38, -6.5);
    private static final Pos RIGHT = new Pos(-32.5, 38, -14.5);

    private static final double BASE_OFFSET = 1.8;
    private static final int ENTITY_UPDATE_INTERVAL = 5; // Seconds

    private final Controller guiController;

    private final NpcItemModel[] edgeEntities = new NpcItemModel[5];
    private int entityHeightTarget = 0;

    @Inject
    public PlayMapStatueFeatureProvider(@NotNull HubMapWorld world, @NotNull Scheduler scheduler, @NotNull Controller guiController) {
        this.guiController = guiController;

        edgeEntities[0] = new NpcItemModel();
        edgeEntities[0].setModel(Material.DIAMOND, BadSprite.require("icon/map/create_map").cmd());
        edgeEntities[0].setHandler(this::handleCreateMapsClick);
        edgeEntities[0].setInstance(world.instance(), LEFT);

        edgeEntities[3] = new NpcItemModel();
        edgeEntities[3].setModel(Material.DIAMOND, BadSprite.require("icon/map/best").cmd());
        edgeEntities[3].setHandler(this::handleBestMapsClick);
        edgeEntities[3].setInstance(world.instance(), LEFT_MIDDLE);

        edgeEntities[4] = new NpcItemModel();
        edgeEntities[4].getEntityMeta().setItemStack(ItemStack.of(Material.STICK)
                .with(ItemComponent.CUSTOM_MODEL_DATA, 14)
                .with(ItemComponent.DYED_COLOR, new DyedItemColor(new Color(0xFF0000))));
        edgeEntities[4].getEntityMeta().setScale(new Vec(5)); // 5x because its 5x5x5
        edgeEntities[4].getEntityMeta().setTranslation(new Vec(0, 1 + BASE_OFFSET, 0));
        edgeEntities[4].setInstance(world.instance(), MIDDLE.withView(180, 0));
        edgeEntities[4].setHandler(this::handleQualityMapsClick);
        edgeEntities[4].setInteractionBox(6, 6);

        edgeEntities[1] = new NpcItemModel();
        edgeEntities[1].setModel(Material.DIAMOND, BadSprite.require("icon/map/new").cmd());
        edgeEntities[1].setHandler(this::handleNewMapsClick);
        edgeEntities[1].setInstance(world.instance(), RIGHT_MIDDLE);

        edgeEntities[2] = new NpcItemModel();
        edgeEntities[2].setModel(Material.DIAMOND, BadSprite.require("icon/map/search").cmd());
        edgeEntities[2].setHandler(this::handleSearchMapsClick);
        edgeEntities[2].setInstance(world.instance(), RIGHT);

        // Configure the edge entities to be the same (not the center)
        for (int i = 0; i < 4; i++) {
            edgeEntities[i].setInteractionBox(4, 5);
            var meta = edgeEntities[i].getEntityMeta();
            meta.setScale(new Vec(3));
            meta.setTranslation(new Vec(0, BASE_OFFSET, 0));
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
        }

        scheduler.submitTask(this::entityUpdate, ExecutionType.TICK_START);
    }

    private void handleCreateMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand, boolean isLeftClick) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, CreateMaps::new);
    }

    private void handleBestMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand, boolean isLeftClick) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, c -> new PlayMapsView(c.with(Map.of("query", "")), PlayMapsView.SortPreset.BEST));
    }

    private void handleQualityMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand, boolean isLeftClick) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, c -> new PlayMapsView(c.with(Map.of("query", "")), PlayMapsView.SortPreset.APPROVED));
    }

    private void handleNewMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand, boolean isLeftClick) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, c -> new PlayMapsView(c.with(Map.of("query", "")), PlayMapsView.SortPreset.RECENT));
    }

    private void handleSearchMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand, boolean isLeftClick) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, QueryMapsView::new);
    }

    private @NotNull TaskSchedule entityUpdate() {
        int updateInterval = (int) (20 * 0.5) * ENTITY_UPDATE_INTERVAL;

        for (int i = 0; i < edgeEntities.length; i++) {
            var meta = edgeEntities[i].getEntityMeta();
            meta.setNotifyAboutChanges(false);

            meta.setTransformationInterpolationStartDelta(0);
            meta.setTransformationInterpolationDuration(updateInterval);

            double verticalOffset = ((entityHeightTarget + i) % 8) * (1 / 8.0);
            if (verticalOffset > 0.5) verticalOffset = 1 - verticalOffset;
            meta.setTranslation(new Vec(0, (i == 4 ? 1 : 0) + BASE_OFFSET + (verticalOffset * 0.75), 0));

            meta.setNotifyAboutChanges(true);
        }

        entityHeightTarget += 1;
        return TaskSchedule.tick(updateInterval);
    }

}
