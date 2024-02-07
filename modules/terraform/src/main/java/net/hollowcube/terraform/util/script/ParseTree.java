package net.hollowcube.terraform.util.script;

import org.jetbrains.annotations.NotNull;

public interface ParseTree<T> {

    int start();

    int end();

    /**
     * Returns the parsed value, or an exception if the parse tree is invalid.
     *
     * @return the parsed value
     * @throws ParseException if the parse tree is invalid
     */
    @NotNull T into() throws ParseException;

}
