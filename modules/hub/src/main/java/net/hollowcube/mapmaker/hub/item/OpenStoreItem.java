package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.store.StoreModule;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class OpenStoreItem extends ItemHandler {
    private static final Logger logger = LoggerFactory.getLogger(OpenStoreItem.class);

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/store_menu"));
    public static final String ID = "mapmaker:store";

    private final MapServer server;

    public OpenStoreItem(@NotNull MapServer server) {
        super(ID, RIGHT_CLICK_ANY);
        this.server = server;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        try {
            final ScriptEngine scriptEngine = server.scriptEngine();
            scriptEngine.guiManager().openGui(click.player(), URI.create("guilib:///store/store-view.js"), Map.of(
                    "@mapmaker/internal/store", new StoreModule(server.playerService(), click.player())
            ));
        } catch (Exception e) {
            logger.error("failed to open store view", e);
            ExceptionReporter.reportException(e, click.player());
        }
    }

}
