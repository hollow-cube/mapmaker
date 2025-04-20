package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.PersonalizedMapData;

public interface ProgressMapEntry {

    MapData map();

    void setProgress(PersonalizedMapData.Progress progress, int playtime);
}
