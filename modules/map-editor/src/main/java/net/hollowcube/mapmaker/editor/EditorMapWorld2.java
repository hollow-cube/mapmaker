package net.hollowcube.mapmaker.editor;

import net.hollowcube.mapmaker.map.AbstractMapWorld2;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapServer;
import org.jetbrains.annotations.NotNullByDefault;

@NotNullByDefault
public class EditorMapWorld2 extends AbstractMapWorld2 {

    public EditorMapWorld2(MapServer server, MapData map) {
        super(server, map, makeMapInstance(map, 'e'));
    }

}
