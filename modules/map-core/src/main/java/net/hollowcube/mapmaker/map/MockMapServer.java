package net.hollowcube.mapmaker.map;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandManager;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.punishments.PunishmentService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Function;

public class MockMapServer implements MapServer {
    public ApiClient api;
    public SessionService sessionService;
    public PlayerService playerService;
    public MapService mapService;
    public PunishmentService punishmentService;
    public PlayerInviteService inviteService;
    public SessionManager sessionManager;
    public ServerBridge bridge;
    public Controller guiController;
    public Scheduler scheduler;
    public CommandManager commandManager;

    @Override
    public @NotNull ApiClient api() {
        return Objects.requireNonNull(api, "API Client is not initialized");
    }

    @Override
    public @NotNull SessionService sessionService() {
        return Objects.requireNonNull(sessionService, "SessionService is not initialized");
    }

    @Override
    public @NotNull PlayerService playerService() {
        return Objects.requireNonNull(playerService, "PlayerService is not initialized");
    }

    @Override
    public @NotNull MapService mapService() {
        return Objects.requireNonNull(mapService, "MapService is not initialized");
    }

    @Override
    public @NotNull PunishmentService punishmentService() {
        return Objects.requireNonNull(punishmentService, "PunishmentService is not initialized");
    }

    @Override
    public @NotNull PlayerInviteService inviteService() {
        return Objects.requireNonNull(inviteService, "PlayerInviteService is not initialized");
    }

    @Override
    public @NotNull SessionManager sessionManager() {
        return Objects.requireNonNull(sessionManager, "SessionManager is not initialized");
    }

    @Override
    public @NotNull ServerBridge bridge() {
        return Objects.requireNonNull(bridge, "ServerBridge is not initialized");
    }

    @Override
    public @NotNull CommandManager commandManager() {
        return Objects.requireNonNull(commandManager, "CommandManager is not initialized");
    }

    @Override
    public @NotNull Controller guiController() {
        return Objects.requireNonNull(guiController, "GUI Controller is not initialized");
    }

    @Override
    public @NotNull Scheduler scheduler() {
        return Objects.requireNonNull(scheduler, "Scheduler is not initialized");
    }

    @Override
    public <T> @NotNull T facet(@NotNull Class<T> type) {
        throw new IllegalArgumentException("MockMapServer does not support facets");
    }

    @Override
    public void showView(@NotNull Player player, @NotNull Function<Context, View> viewProvider) {
        guiController().show(player, viewProvider);
    }
}
