package net.hollowcube.mapmaker.map.obungus.ui;

import net.hollowcube.mapmaker.gui.world.IWGColumnElement;
import net.hollowcube.mapmaker.gui.world.InWorldGui;
import net.hollowcube.mapmaker.obungus.ObungusBoxData;
import net.hollowcube.mapmaker.obungus.ObungusService;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public class MapReviewGui extends InWorldGui {

    private final ObungusService obungus;
    private final ObungusBoxData box;

    public MapReviewGui(@NotNull ObungusService obungus, @NotNull ObungusBoxData box) {
        this.obungus = obungus;
        this.box = box;

        root = new IWGColumnElement()
                .addChild(new MapReviewRatingElement(Component.text("Difficulty")))
                .addChild(new MapReviewRatingElement(Component.text("Quality")));

    }
}
