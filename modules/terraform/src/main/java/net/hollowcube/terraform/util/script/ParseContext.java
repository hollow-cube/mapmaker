package net.hollowcube.terraform.util.script;

import net.hollowcube.terraform.TerraformRegistry;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ParseContext(
        @NotNull TerraformRegistry registry,
        @Nullable Player player
) {

    public static final @NotNull ParseContext EMPTY = new ParseContext(TerraformRegistry.EMPTY, null);

    public static @NotNull ParseContext of(@NotNull TerraformRegistry registry) {
        return new ParseContext(registry, null);
    }

    public static @NotNull ParseContext of(@NotNull TerraformRegistry registry, @NotNull Player player) {
        return new ParseContext(registry, player);
    }

    public @NotNull Player getPlayer() throws ParseException {
        if (this.player == null) {
            throw new ParseException("Player in context is required");
        }
        return this.player;
    }
}
