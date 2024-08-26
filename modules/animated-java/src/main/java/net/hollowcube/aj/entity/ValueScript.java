package net.hollowcube.aj.entity;

import net.hollowcube.mql.foreign.MqlEnv;

public interface ValueScript {

    double eval(@MqlEnv({"query", "q"}) AnimQuery query);

}
