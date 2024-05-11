package net.hollowcube.command.arg;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArgumentItemStack extends Argument<ItemStack> {
    private final IntSet disallowedItemComponents = IntSets.emptySet(); // Allow all by default

    ArgumentItemStack(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<ItemStack> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return null;
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        super.suggest(sender, raw, suggestion);
    }
}
