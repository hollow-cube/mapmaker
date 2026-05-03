package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.common.ServerRuntime;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVerification;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NotNullByDefault
final class MapPublisher {

    private final ApiClient api;
    private final MapService mapService;
    private final ServerBridge bridge;
    private final MapData map;

    private final Button button;

    @Blocking MapPublisher(ApiClient api, MapService mapService, ServerBridge bridge, MapData map,
                 Supplier<InventoryHost> hostSupplier, Consumer<MapData> onPublish) {
        this.api = api;
        this.mapService = mapService;
        this.bridge = bridge;
        this.map = map;

        this.button = new Button(3, 3)
            .onLeftClickAsync(() -> this.onVerifyPublish(hostSupplier.get(), onPublish));
        this.updateStage();
    }

    Button getButton() {
        return this.button;
    }

    @Blocking
    void updateStage() {
        var currentStage = this.getCurrentStage();
        this.button.translationKey(currentStage.translationKey).background(currentStage.background);
    }

    @Blocking
    private void onVerifyPublish(InventoryHost host, Consumer<MapData> onPublish) {
        var currentStage = this.getCurrentStage();
        if (currentStage == PublishStage.VERIFICATION_READY) {
            this.verifyMap(host);
        }
        if (currentStage == PublishStage.PUBLISH_READY) {
            this.publishMap(host.player(), onPublish);
        }
    }

    @Blocking
    private void verifyMap(InventoryHost host) {
        if (this.map.verification() == MapVerification.UNVERIFIED) {
            this.tryBeginVerification(host);
        }

        try {
            host.close();
            this.bridge.joinMap(host.player(), this.map.id(), ServerBridge.JoinMapState.VERIFYING, "edit_maps_gui_verify");
        } catch (Exception exception) {
            host.player().sendMessage(Component.translatable("map.verify.fail"));
            ExceptionReporter.reportException(exception, host.player());
        }
    }

    @Blocking
    private void tryBeginVerification(InventoryHost host) {
        var player = host.player();
        try {
            api.maps.beginVerification(this.map.id());
        } catch (Exception exception) {
            host.close();
            host.player().sendMessage(Component.translatable("edit.map.failure"));
            ExceptionReporter.reportException(exception, player);
        }
    }

    @Blocking
    private void publishMap(Player player, Consumer<MapData> onPublish) {
        MapData publishedMap = this.doPublish(player);
        if (publishedMap == null) return; // There was an error

        onPublish.accept(publishedMap);
    }

    private void publishContestMap() {
        // TODO: when we decide about contest maps, implement this
    }

    @Blocking
    private @Nullable MapData doPublish(Player player) {
        MapData result = null;
        try {
            // Save any pending changes immediately so details has the correct data (and we dont modify the map after publish)
            map.settings().withUpdateRequest(req -> {
                api.maps.update(map.id(), req);
                return true;
            });

            api.maps.publish(map.id());

            // TODO(v4 api): we refetch the map so it includes leaderboard info
            result = api.maps.get(map.id());
        } catch (Exception exception) {
            player.sendMessage(Component.translatable("publish.map.failure"));
            ExceptionReporter.reportException(exception, player);
        }
        return result;
    }

    @Blocking
    private PublishStage getCurrentStage() {
        long currentPlaytime;
        try {
            var saveState = this.mapService.getLatestSaveState(this.map.id(), this.map.owner(), SaveStateType.EDITING, null);
            currentPlaytime = saveState.getPlaytime();
        } catch (MapService.NotFoundError _) {
            return PublishStage.ERROR_BUILD_AMOUNT;
        }

        if (!this.map.isVerified()) return PublishStage.VERIFICATION_READY;

        if (this.map.settings().getName().isEmpty()) return PublishStage.ERROR_NO_NAME;
        if (this.map.settings().getIcon() == null) return PublishStage.ERROR_NO_ICON;
        // TODO: Not sure if this is right or not - must check. Surely they need to set at least one **gameplay**
        //  tag, not just one of any tag, right?
        if (this.map.settings().getTags().isEmpty()) return PublishStage.ERROR_NO_TAG;

        // Check playtime down here so we don't get a rogue publish error show up because they haven't built in
        // it for long enough
        var minPlaytime = getMinPlaytime();
        if (currentPlaytime < minPlaytime) {
            return PublishStage.ERROR_BUILD_TIME;
        }

        return PublishStage.PUBLISH_READY;
    }

    private static final int DEFAULT_MIN_PLAYTIME = ServerRuntime.getRuntime().isDevelopment()
        ? 1 // In development, we skip min playtime entirely
        : (int) Duration.ofMinutes(5).toMillis();

    private static int getMinPlaytime() {
        return System.getenv("MIN_PLAYTIME") == null ? DEFAULT_MIN_PLAYTIME : 0;
    }

    private enum PublishStage {
        ERROR_BUILD_AMOUNT("gui.create_maps.edit.verify.error.build_amount", "create_maps2/edit/verify_red"),
        VERIFICATION_READY("gui.create_maps.edit.verify", "create_maps2/edit/verify_orange"),

        ERROR_NO_NAME("gui.create_maps.edit.publish.error.no_name", "create_maps2/edit/publish_red"),
        ERROR_NO_ICON("gui.create_maps.edit.publish.error.no_icon", "create_maps2/edit/publish_red"),
        ERROR_NO_TAG("gui.create_maps.edit.publish.error.no_tag", "create_maps2/edit/publish_red"),
        ERROR_BUILD_TIME("gui.create_maps.edit.publish.error.build_time", "create_maps2/edit/publish_red"),
        PUBLISH_READY("gui.create_maps.edit.publish", "create_maps2/edit/publish_green");

        final String translationKey;
        final String background;

        PublishStage(String translationKey, String background) {
            this.translationKey = translationKey;
            this.background = background;
        }
    }
}
