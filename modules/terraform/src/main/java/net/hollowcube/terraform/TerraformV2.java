package net.hollowcube.terraform;

import org.jetbrains.annotations.NotNull;

/**
 * TerraformV2 is the root of a Terraform instance.
 * <p>
 * It should be created once for an entire server
 */
public sealed interface TerraformV2 permits TerraformImpl {

    class StaticAbuse {
        public static TerraformV2 instance = TerraformV2.builder().build();
    }

    static @NotNull Builder builder() {
        return new Builder();
    }


    class Builder {

        public @NotNull TerraformV2 build() {
            return new TerraformImpl();
        }
    }
}
