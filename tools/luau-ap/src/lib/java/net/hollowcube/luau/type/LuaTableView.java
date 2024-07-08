package net.hollowcube.luau.type;

import org.jetbrains.annotations.NotNull;

public interface LuaTableView {

    //todo


    @NotNull Iterator iterator();

    interface Iterator {
        boolean hasNext();

        // Keys
        @NotNull String getStringKey();

        // Values
        @NotNull String getStringValue();
    }
}
