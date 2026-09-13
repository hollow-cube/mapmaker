package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.molang.MolangEnvironment;
import net.hollowcube.molang.MolangProgram;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.Nullable;

public record MolangExpression<T>(
        String text,
        @Nullable MolangProgram<T> program,
        @Nullable Throwable error
) {

    public static <T> Codec<MolangExpression<T>> codec(MolangEnvironment<T> environment) {
        return Codec.STRING.transform(text -> from(environment, text), MolangExpression::text);
    }

    // TODO syntax highlighting
    public Component display() {
        return Component.text(this.text);
    }

    public static <T> MolangExpression<T> from(MolangEnvironment<T> environment, String text) {
        try {
            return new MolangExpression<>(text, environment.compile(text), null);
        } catch (RuntimeException e) {
            return new MolangExpression<>(text, null, e);
        }
    }
}
