package net.hollowcube.aj.model;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.aj.entity.ValueScript;
import net.hollowcube.aj.util.ExtraCodecs;
import net.hollowcube.aj.util.MqlCompilerOps;
import net.hollowcube.mql.MqlCompiler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public sealed interface DynamicKeyframe {

    enum Channel {
        POSITION,
        ROTATION,
        SCALE,
        VARIANT,
        COMMANDS;

        public static final Codec<Channel> CODEC = ExtraCodecs.enumString(Channel.class);

        public @NotNull Codec<? extends DynamicKeyframe> channelCodec() {
            return switch (this) {
                case POSITION, ROTATION, SCALE -> Vec3.CODEC;
                case VARIANT -> Variant.CODEC;
                case COMMANDS -> Commands.CODEC;
            };
        }
    }

    double time();

    @NotNull Channel channel();

    Codec<DynamicKeyframe> CODEC = Channel.CODEC.dispatch("channel", DynamicKeyframe::channel, Channel::channelCodec);

    record ScriptTriple(
            @NotNull MqlCompiler.Unit<ValueScript> x,
            @NotNull MqlCompiler.Unit<ValueScript> y,
            @NotNull MqlCompiler.Unit<ValueScript> z
    ) {
        private static final Codec<MqlCompiler.Unit<ValueScript>> SCRIPT_UNIT_CODEC = new Codec<>() {
            @Override
            public <T> DataResult<Pair<MqlCompiler.Unit<ValueScript>, T>> decode(DynamicOps<T> ops, T input) {
                String script = ops.getStringValue(input).result().orElseThrow();
                if (!(ops instanceof MqlCompilerOps<T> mqlOps))
                    throw new IllegalArgumentException("Unsupported ops: " + ops);
                var unit = mqlOps.compiler().addScript(ValueScript.class, script, true, null);
                return DataResult.success(Pair.of(unit, ops.empty()));
            }

            @Override
            public <T> DataResult<T> encode(MqlCompiler.Unit<ValueScript> input, DynamicOps<T> ops, T prefix) {
                throw new UnsupportedOperationException();
            }
        };

        // array of 3 strings
        public static final Codec<ScriptTriple> CODEC = SCRIPT_UNIT_CODEC.listOf().xmap(
                list -> {
                    if (list.size() != 3)
                        throw new IllegalArgumentException("Expected 3 scripts, got " + list.size());
                    return new ScriptTriple(list.get(0), list.get(1), list.get(2));
                },
                triple -> List.of(triple.x(), triple.y(), triple.z()));
    }

    record Vec3(
            double time,
            @NotNull Channel channel,
            @NotNull ScriptTriple value
            //todo: post,
            //todo: interpolation
    ) implements DynamicKeyframe {
        public static final Codec<Vec3> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("time").forGetter(Vec3::time),
                Channel.CODEC.fieldOf("channel").forGetter(Vec3::channel),
                ScriptTriple.CODEC.fieldOf("value").forGetter(Vec3::value)
        ).apply(i, Vec3::new));
    }

    record Variant(
            double time,
            @NotNull UUID variant,
            @NotNull String executeCondition
    ) implements DynamicKeyframe {
        public static final Codec<Variant> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("time").forGetter(Variant::time),
                ExtraCodecs.UUID_STRING.fieldOf("variant").forGetter(Variant::variant),
                Codec.STRING.optionalFieldOf("execute_condition", "").forGetter(Variant::executeCondition)
        ).apply(i, Variant::new));

        @Override
        public @NotNull Channel channel() {
            return Channel.VARIANT;
        }
    }

    record Commands(
            double time,
            @NotNull String commands,
            @NotNull String executeCondition,
            boolean repeat,
            double repeatFrequency
    ) implements DynamicKeyframe {
        public static final Codec<Commands> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("time").forGetter(Commands::time),
                Codec.STRING.fieldOf("commands").forGetter(Commands::commands),
                Codec.STRING.optionalFieldOf("execute_condition", "").forGetter(Commands::executeCondition),
                Codec.BOOL.optionalFieldOf("repeat", false).forGetter(Commands::repeat),
                Codec.DOUBLE.optionalFieldOf("repeat_frequency", 1.0).forGetter(Commands::repeatFrequency)
        ).apply(i, Commands::new));

        @Override
        public @NotNull Channel channel() {
            return Channel.COMMANDS;
        }
    }
}
