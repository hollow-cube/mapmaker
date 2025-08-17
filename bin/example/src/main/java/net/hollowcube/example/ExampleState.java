package net.hollowcube.example;

import net.hollowcube.mapmaker.map.PlayerState;

public sealed interface ExampleState extends PlayerState<ExampleState, ExampleWorld> {

    record Main() implements ExampleState {

    }

}
