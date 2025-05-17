package net.hollowcube.mapmaker.map.block.custom.bouncepad;

import net.hollowcube.common.util.Either;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mql.jit.BouncePadScript;
import net.hollowcube.mql.jit.MqlCompiler;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;

@SuppressWarnings("UnstableApiUsage")
public sealed interface BouncePadData extends DebugCommand.BlockDebug {

    double DEFAULT_POWER = 25;
    double MAX_VELOCITY = 4096;

    // Will try to parse the molang format first and then will go to legacy
    StructCodec<BouncePadData> LEGACY_CODEC = StructCodec.struct(
            "power", Codec.DOUBLE.optional(DEFAULT_POWER), data -> data instanceof Simple simple ? simple.power : DEFAULT_POWER,
            "legacy_cylone_mode", Codec.BOOLEAN.optional(false), data -> data instanceof Cylone,
            (power, legacyCylone) -> legacyCylone ? new Cylone() : new Simple(power));
    Codec<BouncePadData> CODEC = ExtraCodecs.either(Molang.CODEC, LEGACY_CODEC).transform(
            either -> either.map(it -> it, it -> it),
            data -> data instanceof Molang molang ? Either.left(molang) : Either.right(data));

    @Nullable Vec getVelocity(@NotNull Player player);

    default void onUpdate(@Nullable Player player) {

    }

    record Simple(double power) implements BouncePadData {

        @Override
        public @NotNull Vec getVelocity(@NotNull Player player) {
            return player.getPosition().direction().withY(1).mul(this.power());
        }

        @Override
        public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
            player.sendMessage("Power: " + this.power());
        }
    }

    record Cylone() implements BouncePadData {

        @Override
        public @NotNull Vec getVelocity(@NotNull Player player) {
            return Vec.fromPoint(player.getPosition().sub(player.getPreviousPosition()).withY(1.45f * 20));
        }

        @Override
        public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
            player.sendMessage("Cyclone: true");
        }
    }

    final class Molang implements BouncePadData {
        private static final Codec<Molang> CODEC = StructCodec.struct(
                "dx", Codec.STRING, data -> data.dxScript,
                "dy", Codec.STRING, data -> data.dyScript,
                "dz", Codec.STRING, data -> data.dzScript,
                Molang::new);

        // TODO: replace with stable value when that exists. dont currently support mql with native image
        private static MqlCompiler<BouncePadScript> COMPILER;

        private static @NotNull MqlCompiler<BouncePadScript> compiler() {
            if (COMPILER == null) {
                try {
                    COMPILER = new MqlCompiler<>(MethodHandles.privateLookupIn(BouncePadScript.class, MethodHandles.lookup()), BouncePadScript.class);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return COMPILER;
        }

        private final String dxScript;
        private final String dyScript;
        private final String dzScript;

        private final BouncePadScript.Queries queries = new BouncePadScript.Queries();
        private final BouncePadScript.Variables variables = new BouncePadScript.Variables();

        private BouncePadScript dx;
        private BouncePadScript dy;
        private BouncePadScript dz;
        private boolean failedCompilation;

        public Molang(String dxScript, String dyScript, String dzScript) {
            this.dxScript = dxScript;
            this.dyScript = dyScript;
            this.dzScript = dzScript;
        }

        @Override
        public void onUpdate(@Nullable Player player) {
            try {
                var compiler = compiler();
                this.dx = compiler.compile(this.dxScript).newInstance();
                this.dy = compiler.compile(this.dyScript).newInstance();
                this.dz = compiler.compile(this.dzScript).newInstance();
            } catch (Exception e) {
                this.failedCompilation = true;
                if (player != null) {
                    player.sendMessage("Failed to compile script: " + e.getMessage());
                }
            }
        }

        @Override
        public @Nullable Vec getVelocity(@NotNull Player player) {
            // Sanity check just in case
            if ((this.dx == null || this.dy == null || this.dz == null) && !this.failedCompilation) this.onUpdate(null);
            if (this.failedCompilation) return null;

            var pos = player.getPosition();
            var vel = player.getVelocity();

            this.queries.player = player;
            this.variables.x = pos.x();
            this.variables.y = pos.y();
            this.variables.z = pos.z();
            this.variables.dx = vel.x();
            this.variables.dy = vel.y();
            this.variables.dz = vel.z();
            this.variables.yaw = pos.yaw();
            this.variables.pitch = pos.pitch();

            return new Vec(
                    this.dx.eval(this.queries, this.variables),
                    this.dy.eval(this.queries, this.variables),
                    this.dz.eval(this.queries, this.variables)
            );
        }

        @Override
        public void sendDebugInfo(@NotNull Player player, @NotNull Block block) {
            player.sendMessage("Molang: true");
            player.sendMessage("dx: " + this.dxScript);
            player.sendMessage("dy: " + this.dyScript);
            player.sendMessage("dz: " + this.dzScript);
        }
    }
}
