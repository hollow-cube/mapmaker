package net.hollowcube.mapmaker.map.util.datafix;

import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4326 extends DataVersion {
    public V4326() {
        super(4326);

        addFix(HCDataTypes.PLAY_STATE, V4326::fixPlayStateToActionMap);
    }

    private static Value fixPlayStateToActionMap(Value playState) {
        // ghostBlocks, history, lastState, pos left alone.

//        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));
//        playState.put("mapmaker:progress_index", playState.remove("timeLimit"));
//        playState.put("mapmaker:progress_index", playState.remove("resetHeight"));
//        // todo potions
//        playState.put("mapmaker:progress_index", playState.remove("maxLives"));
//        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));
//        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));
//        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));
//        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));
//        playState.put("mapmaker:progress_index", playState.remove("progressIndex"));

        return playState;
    }
}
