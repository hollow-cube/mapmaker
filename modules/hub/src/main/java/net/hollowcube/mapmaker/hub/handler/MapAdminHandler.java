package net.hollowcube.mapmaker.hub.handler;

import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MapAdminHandler {
    //todo merge with Handler
    private static final Logger logger = LoggerFactory.getLogger(MapAdminHandler.class);

    private final MapStorage storage;

    public MapAdminHandler(@NotNull MapStorage storage) {
        this.storage = storage;
    }

    //todo delete me, need a more general map query builder
    public void showMapList(@NotNull Player player, @Nullable String playerId) {
        //todo need to validate players existence
        FutureResult<List<MapData>> future = playerId == null ? FutureResult.error(Error.of("not implemented")) : storage.getMapsByPlayer(playerId);
        future.then(maps -> {
            if (maps.isEmpty()) {
                List<Component> msg;
                if (playerId != null)
                    msg = LanguageProvider.createMultiTranslatable("command.map.admin.list.empty.player", Component.text(playerId));
                else msg = LanguageProvider.createMultiTranslatable("command.map.admin.list.empty");
                msg.forEach(player::sendMessage);
                return;
            }

            var msg = playerId != null ? LanguageProvider.createMultiTranslatable("command.map.admin.list.header.player", Component.text(playerId))
                    : LanguageProvider.createMultiTranslatable("command.map.admin.list.header");
            msg.forEach(player::sendMessage);

            for (MapData map : maps) {
                var entryMsg = LanguageProvider.createMultiTranslatable("command.map.admin.list.entry",
                        Component.text(map.getId()).clickEvent(ClickEvent.copyToClipboard(map.getId())).hoverEvent(Component.text("Click to copy")),
                        Component.text(map.getName()));
                entryMsg.forEach(player::sendMessage);
            }
        });

    }

    //todo delete me
    public void showMapInfoById(@NotNull Player player, @NotNull String mapId) {
        storage.getMapById(mapId)
                .then(map -> {
                    var msg = LanguageProvider.createMultiTranslatable("command.map.admin.info.map",
                            Component.text(map.getId()).clickEvent(ClickEvent.copyToClipboard(map.getId())).hoverEvent(Component.text("Click to copy")),
                            Component.text(map.getName()),
                            Component.text(map.getOwner()));
                    msg.forEach(player::sendMessage);
                })
                .mapErr(err -> {
                    if (err.is(MapStorage.ERR_NOT_FOUND)) {
                        // Specific error for map not found
                        var msg = LanguageProvider.createMultiTranslatable("command.map.admin.info.not_found", Component.text(mapId));
                        msg.forEach(player::sendMessage);
                    } else {
                        // Some other error
                        var msg = LanguageProvider.createMultiTranslatable("command.map.admin.info.unknown_error",
                                Component.text(err.message()), Component.text(mapId));
                        msg.forEach(player::sendMessage);
                        logger.error("failed to fetch map info for {}: {}", mapId, err);
                    }
                    return Result.ofNull();
                });
    }

}
