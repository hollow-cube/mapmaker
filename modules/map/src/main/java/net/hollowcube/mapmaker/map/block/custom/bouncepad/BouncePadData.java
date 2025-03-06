package net.hollowcube.mapmaker.map.block.custom.bouncepad;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.map.command.DebugCommand;
import net.hollowcube.mql.jit.BouncePadScript;
import net.hollowcube.mql.jit.MqlCompiler;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

public sealed interface BouncePadData extends DebugCommand.BlockDebug {

    double DEFAULT_POWER = 25;

    // Will try to parse the molang format first and then will go to legacy
    Codec<BouncePadData> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.lenientOptionalFieldOf("power", DEFAULT_POWER).forGetter(data -> data instanceof Simple(double power) ? power : DEFAULT_POWER),
            Codec.BOOL.lenientOptionalFieldOf("legacy_cylone_mode", false).forGetter(data -> data instanceof Cylone)
    ).apply(instance, (power, legacyCylone) -> legacyCylone ? new Cylone() : new Simple(power)));
    Codec<BouncePadData> CODEC = Codec.either(Molang.CODEC, LEGACY_CODEC).xmap(
            either -> either.map(Function.identity(), Function.identity()),
            data -> data instanceof Molang molang ? Either.left(molang) : Either.right(data)
    );

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

        private static final Codec<Molang> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("dx").forGetter(data -> data.dxScript),
                Codec.STRING.fieldOf("dy").forGetter(data -> data.dyScript),
                Codec.STRING.fieldOf("dz").forGetter(data -> data.dzScript)
        ).apply(instance, Molang::new));

        private static final MqlCompiler<BouncePadScript> COMPILER;
        static {
            try {
                COMPILER = new MqlCompiler<>(MethodHandles.privateLookupIn(BouncePadScript.class, MethodHandles.lookup()), BouncePadScript.class);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
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
                this.dx = COMPILER.compile(this.dxScript).newInstance();
                this.dy = COMPILER.compile(this.dyScript).newInstance();
                this.dz = COMPILER.compile(this.dzScript).newInstance();
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
