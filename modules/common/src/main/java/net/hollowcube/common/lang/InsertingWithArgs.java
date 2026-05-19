package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface InsertingWithArgs extends Inserting {
    @NotNull Component value(@NotNull List<ComponentLike> args);

    @Override
    default Component value() {
        return value(List.of());
    }
}
