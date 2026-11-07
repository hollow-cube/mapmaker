package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.mapmaker.map.PlayerState;

public sealed interface FreeformState extends PlayerState<FreeformState, FreeformMapWorld> {

    record Playing() implements FreeformState {}
}
