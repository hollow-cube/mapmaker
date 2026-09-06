package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.ipc.map.BeginVerificationResult;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.ipc.map.MapStatus;
import net.hollowcube.ipc.map.PublishMapResult;
import net.hollowcube.ipc.map.PublishRequirement;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapWriteMessages;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

@NotNullByDefault
final class MapPublisher {

    private final AtomicBoolean submitting = new AtomicBoolean();
    private final ApiClient api;
    private final ServerBridge bridge;
    private final MapPatch.Builder editor;

    private final Button button;

    @Blocking MapPublisher(
        ApiClient api, ServerBridge bridge, MapPatch.Builder editor,
        Supplier<InventoryHost> hostSupplier, Consumer<MapData> onPublish
    ) {
        this.api = api;
        this.bridge = bridge;
        this.editor = editor;

        this.button = new Button(3, 3)
            .onLeftClickAsync(() -> this.onVerifyPublish(hostSupplier.get(), onPublish));
        this.updateStage();
    }

    Button getButton() {
        return this.button;
    }

    @Blocking
    void updateStage() {
        var currentStage = this.currentStage();
        this.button.translationKey(currentStage.translationKey).background(currentStage.background);
    }

    @Blocking
    private void onVerifyPublish(InventoryHost host, Consumer<MapData> onPublish) {
        if (!submitting.compareAndSet(false, true)) return;
        try {
            var stage = this.currentStage();
            if (stage == PublishStage.VERIFICATION_READY) this.verifyMap(host);
            if (stage == PublishStage.PUBLISH_READY) this.publishMap(host.player(), onPublish);
        } finally {
            submitting.set(false);
        }
    }

    @Blocking
    private void verifyMap(InventoryHost host) {
        if (!this.tryBeginVerification(host)) return;

        try {
            host.close();
            this.bridge.joinMap(host.player(), editor.map().id().toString(), ServerBridge.JoinMapState.VERIFYING, "edit_maps_gui_verify");
        } catch (Exception exception) {
            host.player().sendMessage(Component.translatable("map.verify.fail"));
            ExceptionReporter.reportException(exception, host.player());
        }
    }

    @Blocking
    private boolean tryBeginVerification(InventoryHost host) {
        try {
            var result = api.maps.beginVerification(editor.map().id().toString());
            if (result != BeginVerificationResult.READY) {
                host.player().sendMessage(MapWriteMessages.verification(result));
                return false;
            }
            return true;
        } catch (Exception exception) {
            host.player().sendMessage(Component.translatable("edit.map.failure"));
            ExceptionReporter.reportException(exception, host.player());
            return false;
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
            editor.save(req -> {
                api.maps.update(editor.map().id().toString(), req);
                return editor.map();
            });

            var outcome = api.maps.publish(editor.map().id().toString());
            if (outcome instanceof PublishMapResult.Success(var published)) {
                result = published;
            } else {
                player.sendMessage(MapWriteMessages.failure(outcome));
            }
        } catch (Exception exception) {
            player.sendMessage(Component.translatable("publish.map.failure"));
            ExceptionReporter.reportException(exception, player);
        }
        return result;
    }

    /// Unsaved edits are saved first, so that the api judges the map the player is looking at.
    @Blocking
    private PublishStage currentStage() {
        editor.save(patch -> {
            api.maps.update(editor.map().id().toString(), patch);
            return editor.map();
        });
        var readiness = switch (api.maps.getStatus(editor.map().id().toString())) {
            case MapStatus.Draft(var draft) -> draft;
            case MapStatus.Verifying(var verifying) -> verifying;
            // A map that is gone, or a status this build does not know, is left to the publish
            // call itself to explain.
            case MapStatus.ReadyToPublish _, MapStatus.Published _, MapStatus.NotFound _,
                 MapStatus.Unknown _ -> null;
        };
        if (readiness == null) return PublishStage.PUBLISH_READY;
        // Build time last, so that a map missing a name is told that rather than to keep building;
        // a requirement this build does not know reads as the vaguest of them.
        var missing = readiness.missing();
        if (missing.contains(PublishRequirement.WORLD)) return PublishStage.ERROR_BUILD_AMOUNT;
        if (missing.contains(PublishRequirement.VERIFICATION)) return PublishStage.VERIFICATION_READY;
        if (missing.contains(PublishRequirement.NAME)) return PublishStage.ERROR_NO_NAME;
        if (missing.contains(PublishRequirement.ICON)) return PublishStage.ERROR_NO_ICON;
        if (missing.contains(PublishRequirement.TAGS)) return PublishStage.ERROR_NO_TAG;
        if (missing.contains(PublishRequirement.BUILD_TIME)) return PublishStage.ERROR_BUILD_TIME;
        if (!missing.isEmpty()) return PublishStage.ERROR_BUILD_AMOUNT;
        return PublishStage.PUBLISH_READY;
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
