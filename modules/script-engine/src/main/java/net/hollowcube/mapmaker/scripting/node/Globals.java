package net.hollowcube.mapmaker.scripting.node;

import net.hollowcube.mapmaker.scripting.gui.InventoryHost;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Globals {

    private static final Logger log = LoggerFactory.getLogger(Globals.class);

    public Object setTimeout(@NotNull Value... arguments) {
        if (arguments.length < 2)
            throw new IllegalArgumentException("setTimeout requires at least 2 arguments");
        if (!arguments[0].canExecute())
            throw new IllegalArgumentException("setTimeout requires a function as the first argument");
        if (!arguments[1].isNumber())
            throw new IllegalArgumentException("setTimeout requires a number as the second argument");

        final Value function = arguments[0];
        final long delay = arguments[1].asLong();
        final Value[] args = new Value[arguments.length - 2];
        System.arraycopy(arguments, 2, args, 0, args.length);

        // If zero treat as microtask and return empty timeout id
        if (args.length == 0 && delay <= 0) {
            InventoryHost.current().scheduleMicrotask(function);
            return -1;
        }

        return InventoryHost.current().scheduleTask(function, (int) delay, args);
    }

    public Object clearTimeout(@NotNull Value... arguments) {
        if (arguments.length < 1)
            throw new IllegalArgumentException("clearTimeout requires at least 1 argument");
        if (!arguments[0].isNumber())
            throw new IllegalArgumentException("clearTimeout requires a number as the first argument");

        final Value idArg = arguments[0];
        if (!idArg.isNumber()) return null;
        final int id = idArg.asInt();

        InventoryHost.current().cancelTask(id);
        return null;
    }
}
