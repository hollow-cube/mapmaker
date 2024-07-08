package net.hollowcube.luau.type;

import org.jetbrains.annotations.NotNull;

public interface LuaTableView {

    //todo
    //  var newProps = new HashMap<String, String>();
    //        state.pushNil();
    //        while (state.next(2)) {
    //            // Key is at index -2, value is at index -1
    //            String key = state.toString(-2);
    //            String value = state.toString(-1);
    //            newProps.put(key, value);
    //
    //            // Remove the value, keep the key for the next iteration
    //            state.pop(1);
    //        }
    //
    //        try {
    //            var block = Block.fromStateId(ref);
    //            pushBlock(state, block.withProperties(newProps));
    //            return 1;
    //        } catch (IllegalArgumentException e) {
    //            state.error(e.getMessage());
    //            return 0;
    //        }

    @NotNull Iterator iterator();

    interface Iterator {
        boolean hasNext();

        // Keys
        @NotNull String getStringKey();

        // Values
        @NotNull String getStringValue();
    }
}
