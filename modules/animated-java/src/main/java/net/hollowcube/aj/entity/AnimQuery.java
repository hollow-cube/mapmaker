package net.hollowcube.aj.entity;

import net.hollowcube.mql.builtin.MqlMath;
import net.hollowcube.mql.foreign.ContentErrorException;
import net.hollowcube.mql.foreign.Query;

public record AnimQuery(double lifeTime) {

    @Query("life_time")
    public double lifeTime() {
        return lifeTime;
    }

    @Query
    public double easeinoutquart(double progress) {
        return progress < 0.5
                ? 8 * progress * progress * progress * progress
                : 1 - MqlMath.pow(-2 * progress + 2, 4) / 2;
    }

    @Query("lopsided_wave")
    public double lopsidedWave(double value, double lopsideMag) {
        return MqlMath.sin(value + MqlMath.cos(value) * lopsideMag);
    }

    @Query("linear_wave")
    public double linearWave(double progress) { // double hang
        try {
            double tprogress = progress * (MqlMath.pi() / 180) + MqlMath.pi() * 600;
            double thang = /*hang*/0 * (MqlMath.pi() / 180);
            return MqlMath.mod(MqlMath.abs(tprogress), 2 * MqlMath.pi() + thang * 2) > MqlMath.pi() + thang
                    ? MqlMath.clamp(-MqlMath.mod(MqlMath.abs(tprogress), MqlMath.pi() + thang) / MqlMath.pi() + 1, 0, 1)
                    : MqlMath.clamp(MqlMath.mod(MqlMath.abs(tprogress), MqlMath.pi() + thang) / MqlMath.pi(), 0, 1);
        } catch (ContentErrorException e) {
            return 0;
        }
    }

}
