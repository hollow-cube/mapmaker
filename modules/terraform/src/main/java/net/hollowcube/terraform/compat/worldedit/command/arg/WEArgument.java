package net.hollowcube.terraform.compat.worldedit.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.pattern.Pattern;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.Direction;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public final class WEArgument {

    public static @NotNull Argument<Vec> CommaSeparatedVec3(@NotNull String name) {
        return new ArgumentCommaSeparatedVec3(name);
    }

    public static @NotNull Argument<Vec> CommaSeparatedVec2(@NotNull String name) {
        return new ArgumentCommaSeparatedVec2(name);
    }

    /**
     * Returns an argument to parse a set of the flags in the given enum. The first letter of the enum name is used as the flag.
     *
     * <p>The returned argument always has a default value of an empty enum set.</p>
     *
     * @param enumClass The enum class to derive arguments from.
     * @param <E>       The enum class type.
     * @return The argument with a default empty enum set.
     */
    public static <E extends Enum<E>> @NotNull Argument<EnumSet<E>> FlagSet(@NotNull Class<E> enumClass) {
        return new ArgumentFlagSet<>("flags", enumClass).defaultValue(EnumSet.noneOf(enumClass));
    }

    public static @NotNull Argument<Pattern> Pattern(@NotNull String id) {
        return new ArgumentPattern(id);
    }

    public static @NotNull Argument<Mask> Mask(@NotNull String id) {
        return new ArgumentMask(id);
    }

    public static @NotNull Argument<@NotNull Direction> Direction(@NotNull String id) {
        return new ArgumentDirection(id).defaultValue(ArgumentDirection::getDefault);
    }

    public static @NotNull Argument<DynamicRegistry.Key<Biome>> Biome(@NotNull String id) {
        return new ArgumentBiome(id);
    }

    private WEArgument() {
    }
}
