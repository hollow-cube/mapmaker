package net.hollowcube.map;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.bridge.MapToHubBridge;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.SessionService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.terraform.Terraform;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public interface MapServer {

    class StaticAbuse {
        public static MapServer instance;
    }

    @NotNull MapToHubBridge bridge();

    @NotNull PlayerService playerService();

    @NotNull SessionService sessionService();

    @NotNull MapService mapService();

    @NotNull PlayerInviteService inviteService();

    @NotNull PermManager permManager();

    @NotNull SessionManager sessionManager();

    @NotNull List<FeatureProvider> features();

    @NotNull Terraform terraform();

    void newOpenGUI(@NotNull Player player, @NotNull Function<Context, View> viewProvider);

}
