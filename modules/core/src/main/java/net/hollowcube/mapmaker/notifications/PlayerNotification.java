package net.hollowcube.mapmaker.notifications;

import net.hollowcube.common.components.TranslatableBuilder;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.notifications.impl.PlayerNotificationType;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.util.ServiceContext;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;

public record PlayerNotification(
    PlayerNotificationResponse.ComplexEntry entry,

    Sprite icon,
    Component title,
    List<Component> body,

    List<Action> actions
) {

    public PlayerNotification {
        if (actions.size() > 7) throw new IllegalArgumentException("A notification can have at most 7 actions");
    }

    public PlayerNotification(
        PlayerNotificationResponse.ComplexEntry entry,
        Sprite icon,
        String translation,
        List<Component> args,
        List<Action> actions
    ) {
        this(
            entry,
            icon,
            TranslatableBuilder.of(translation + ".name").withAll(args).toComponent(),
            TranslatableBuilder.of(translation + ".lore").withAll(args).toList(),
            actions
        );
    }

    @NonBlocking
    public static @Nullable PlayerNotification fromResponse(Player player, ServiceContext context, PlayerNotificationResponse.ComplexEntry entry) {
        var type = PlayerNotificationType.Lookup.get(entry.type());
        return type == null ? null : type.createNotification(player, context, entry);
    }

    public Instant createdAt() {
        return this.entry.createdAt();
    }

    public @Nullable Instant readAt() {
        return this.entry.readAt();
    }

    public @Nullable Instant expiresAt() {
        return this.entry.expiresAt();
    }

    public record Action(
        Sprite icon,
        String interaction,
        String tooltip,
        ActionExecutor executor
    ) {

        public static Action of(Sprite icon, String interaction, String tooltip, ActionExecutor executor) {
            return new Action(icon, interaction, tooltip, executor);
        }

        public void executeForRefresh(InventoryHost host, Runnable refresh) {
            this.executor.execute(host, refresh, () -> {});
        }

        public void executeForCompletion(InventoryHost host, Runnable complete) {
            this.executor.execute(host, () -> {}, complete);
        }
    }

    public record ActionExecutor(
        boolean requiresConfirmation,
        boolean requiresRefresh,
        Runnable executor
    ) {

        public static ActionExecutor of(Runnable executor) {
            return new ActionExecutor(false, false, executor);
        }

        public static ActionExecutor ofAsync(Runnable executor) {
            return new ActionExecutor(false, false, () -> FutureUtil.submitVirtual(executor));
        }

        public ActionExecutor withConfirmation() {
            return new ActionExecutor(true, this.requiresRefresh, this.executor);
        }

        public ActionExecutor withRefresh() {
            return new ActionExecutor(this.requiresConfirmation, true, this.executor);
        }

        public void execute(InventoryHost host, Runnable refresh, Runnable complete) {
            if (this.requiresConfirmation) {
                host.pushView(ExtraPanels.confirm(() -> {
                    this.executor.run();
                    if (this.requiresRefresh) refresh.run();
                    complete.run();
                }));
            } else {
                this.executor.run();
                if (this.requiresRefresh) refresh.run();
                complete.run();
            }
        }
    }
}
