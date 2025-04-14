package net.hollowcube.datafix;

import net.hollowcube.datafix.util.Value;

@FunctionalInterface
public interface DataFix {

    Value fix(Value value);

}
