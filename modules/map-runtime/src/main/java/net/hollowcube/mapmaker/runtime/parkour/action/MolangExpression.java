package net.hollowcube.mapmaker.runtime.parkour.action;

import net.hollowcube.molang.MolangExpr;
import net.hollowcube.molang.MolangOptimizer;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.Nullable;

public record MolangExpression(
        String text,
        @Nullable MolangExpr parsed,
        @Nullable Throwable error
) {

    public static final MolangExpression ZERO = new MolangExpression("0", new MolangExpr.Num(0), null);
    public static final Codec<MolangExpression> CODEC = Codec.STRING.transform(
            MolangExpression::from,
            MolangExpression::text
    );

    // TODO syntax highlighting
    public Component display() {
        return Component.text(this.text);
    }

    public static MolangExpression from(String text) {
        try {
            var expr = MolangExpr.parseOrThrow(text);
            return new MolangExpression(text, MolangOptimizer.optimizeAst(expr), null);
        } catch (Throwable t) {
            return new MolangExpression(text, null, t);
        }
    }
}