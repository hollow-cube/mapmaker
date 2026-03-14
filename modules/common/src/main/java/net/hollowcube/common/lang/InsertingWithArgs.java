package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;

import java.util.List;

public interface InsertingWithArgs {
    Component value(List<Component> args);
}
