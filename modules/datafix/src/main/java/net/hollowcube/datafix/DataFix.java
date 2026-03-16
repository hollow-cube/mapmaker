package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface DataFix {

    @Nullable Value fix(Value value);

}
