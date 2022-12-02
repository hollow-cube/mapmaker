package net.hollowcube.terraform.action;

import net.hollowcube.terraform.region.Region;
import org.jetbrains.annotations.NotNull;

public final class ActionBuilder {

    private Region source = null; //todo this should be some generic version of a block source (extent in WE)

    public final class Source {
        private Source() {}

        public @NotNull ActionBuilder.Mod from(@NotNull Region region) {
            source = region;
            return new ActionBuilder.Mod();
        }
    }

    public final class Mod {
        private Mod() {}


    }
}
