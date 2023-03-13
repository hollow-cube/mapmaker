package net.hollowcube.mapmaker.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.model.DisplayNameBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.ServerProcess;
import org.jetbrains.annotations.NotNull;

public class PlayerServiceImpl implements PlayerService, Facet {

    @Override
    public @NotNull ListenableFuture<Void> hook(@NotNull ServerProcess server) {
        return Futures.immediateVoidFuture();
    }

    @Override
    public @NotNull FutureResult<Component> getDisplayName(@NotNull String playerId)  {
        // Implementation Note:
        // This service should aggressively cache the results of this method as it is called very frequently.
        // As a base, all online players should be cached in every server (note: in the future this would likely
        // just be MM players cached in MM), and fetched players should be kept in cache for a reasonable amount
        // of time.
        return FutureResult.of(Component.text(DisplayNameBuilder.getDisplayName(playerId)));
    }

}
