package net.hollowcube.mapmaker.scripting.node;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public final class SetTimeout implements ProxyExecutable {

    @Override
    public Object execute(Value... arguments) {
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

        // TODO: actually sleep :D
//        System.out.println("setTimeout: " + delay + "ms");
        function.executeVoid((Object[]) args);

        return null;
    }

}
