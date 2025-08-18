package net.hollowcube.mapmaker.map.monitoring;

import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;

@SuppressWarnings("rawtypes")
public final class MapCoreJFR {

    public static final String STATE_CHANGE = "mapcore.StateChange";

    @Name(STATE_CHANGE)
    @Label("Map State Change")
    @Description("A player changed state within a map")
    public static final class StateChange extends Event {
        @Label("World Type")
        Class worldType;
        @Label("Last State Type")
        Class lastState;
        @Label("Next State Type")
        Class nextState;

        public StateChange(Class worldType, Class lastState, Class nextState) {
            this.worldType = worldType;
            this.lastState = lastState;
            this.nextState = nextState;
        }
    }

}
