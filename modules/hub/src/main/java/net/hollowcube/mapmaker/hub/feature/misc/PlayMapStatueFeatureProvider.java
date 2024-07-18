package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.gui.play.PlayMapsView;
import net.hollowcube.mapmaker.gui.play.QueryMapsView;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@AutoService(HubFeature.class)
public class PlayMapStatueFeatureProvider implements HubFeature {

    private static final Pos LEFT = new Pos(-32.5, 38, 15.5);
    private static final Pos LEFT_MIDDLE = new Pos(-35.5, 38, 7.5);
    private static final Pos MIDDLE = new Pos(-36.5, 38, 0.5);
    private static final Pos RIGHT_MIDDLE = new Pos(-35.5, 38, -6.5);
    private static final Pos RIGHT = new Pos(-32.5, 38, -14.5);

    private final Controller guiController;

    @Inject
    public PlayMapStatueFeatureProvider(@NotNull HubMapWorld world, @NotNull Controller guiController) {
        this.guiController = guiController;

        var createMapsEntity = BaseNpcEntity.createInteractionEntity(
                4, 5, this::handleCreateMapsClick);
        createMapsEntity.setInstance(world.instance(), LEFT);

        var bestMapsEntity = BaseNpcEntity.createInteractionEntity(
                4, 5, this::handleBestMapsClick);
        bestMapsEntity.setInstance(world.instance(), LEFT_MIDDLE);

        var qualityMapsEntity = BaseNpcEntity.createInteractionEntity(
                4, 5, this::handleQualityMapsClick);
        qualityMapsEntity.setInstance(world.instance(), MIDDLE);

        var newMapsEntity = BaseNpcEntity.createInteractionEntity(
                4, 5, this::handleNewMapsClick);
        newMapsEntity.setInstance(world.instance(), RIGHT_MIDDLE);

        var searchMapsEntity = BaseNpcEntity.createInteractionEntity(
                4, 5, this::handleSearchMapsClick);
        searchMapsEntity.setInstance(world.instance(), RIGHT);

    }

    private void handleCreateMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, CreateMaps::new);
    }

    private void handleBestMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, c -> new PlayMapsView(c.with(Map.of("query", "")), PlayMapsView.SortPreset.BEST));
    }

    private void handleQualityMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, c -> new PlayMapsView(c.with(Map.of("query", "")), PlayMapsView.SortPreset.APPROVED));
    }

    private void handleNewMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, c -> new PlayMapsView(c.with(Map.of("query", "")), PlayMapsView.SortPreset.RECENT));
    }

    private void handleSearchMapsClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull Player.Hand hand) {
        if (hand != Player.Hand.MAIN) return;

        guiController.show(player, QueryMapsView::new);
    }

}
