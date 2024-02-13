package net.hollowcube.terraform.util.script;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.terraform.TerraformRegistry;
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
    default @NotNull T into(@NotNull TerraformRegistry registry) throws ParseException {
        throw new ParseException(start(), end(), "not implemented");
    }

    /**
     * The suggestion position is always assumed to be at the end of the input.
     *
     * @param suggestion the suggestion to add
     */
    default void suggest(@NotNull TerraformRegistry registry, @NotNull Suggestion suggestion) {
        throw new UnsupportedOperationException("suggestions are not supported on " + getClass().getSimpleName());
    }

}
