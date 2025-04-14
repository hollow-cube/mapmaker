package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface FixFunc {

    @NotNull Value fix(Value value);

}
