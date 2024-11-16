package net.hollowcube.mapmaker.map.obungus.ui;

import net.hollowcube.mapmaker.gui.world.IWGColumnElement;
import net.hollowcube.mapmaker.gui.world.IWGRowElement;
import net.hollowcube.mapmaker.gui.world.IWGTextElement;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

// Holds a flat plane with the following:
//        TITLE
// 0 1 2 3 4 5 6 7 8 9
// 0 1 2 3 4 5 6 7 8 9
//
// Where you can select a 0-99 rating (00 selected by default)
public class MapReviewRatingElement extends IWGColumnElement {

    public MapReviewRatingElement(@NotNull Component title) {
        addChild(new IWGTextElement().text(title));

        var tensContainer = new IWGRowElement();
        for (int i = 0; i < 10; i++)
            tensContainer.addChild(new IWGTextElement().text(Component.text(i)));
        addChild(tensContainer);

        var onesContainer = new IWGRowElement();
        for (int i = 0; i < 10; i++)
            onesContainer.addChild(new IWGTextElement().text(Component.text(i)));
        addChild(onesContainer);
    }

}
