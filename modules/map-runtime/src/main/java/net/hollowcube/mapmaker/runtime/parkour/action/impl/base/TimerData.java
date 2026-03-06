package net.hollowcube.mapmaker.runtime.parkour.action.impl.base;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.utils.Either;

import java.util.function.Function;

public record TimerData(int ticks, boolean isPartial) {

    public static final int NO_TIMER = 0; // Disables the timer mechanic.
    public static final int MAX_TIMER = 24 * 60 * 60 * 20; // 24 hours in ticks.
    public static final TimerData ZERO = TimerData.fixed(NO_TIMER);

    private static final Codec<TimerData> FIXED_CODEC = ExtraCodecs
        .clamppedInt(NO_TIMER, MAX_TIMER)
        .transform(TimerData::fixed, TimerData::ticks);
    private static final Codec<TimerData> PARTIAL_CODEC = StructCodec.struct(
        "ticks", ExtraCodecs.clamppedInt(NO_TIMER, MAX_TIMER), TimerData::ticks,
        "partial", Codec.BOOLEAN, TimerData::isPartial,
        TimerData::new
    );

    public static final Codec<TimerData> CODEC = Codec.Either(FIXED_CODEC, PARTIAL_CODEC).transform(
        either -> either.unify(Function.identity(), Function.identity()),
        data -> data.isPartial ? Either.right(data) : Either.left(data)
    );

    public static TimerData fixed(int ticks) {
        return new TimerData(ticks, false);
    }

    public static TimerData partial(int ticks) {
        return new TimerData(ticks, true);
    }

    public TimerData add(int ticks) {
        return new TimerData(this.ticks + ticks, this.isPartial);
    }

    public TimerData sub(int ticks) {
        return new TimerData(Math.max(this.ticks - ticks, 0), this.isPartial);
    }

    public long toMillis() {
        return ticks * 50L;
    }
}
