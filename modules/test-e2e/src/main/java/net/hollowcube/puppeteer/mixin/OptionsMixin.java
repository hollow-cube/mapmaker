package net.hollowcube.puppeteer.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {

    @Inject(method = "<init>", at = @At("RETURN"), order = 10000)
    private void onCreateOptions(CallbackInfo ci) {
        var options = (Options) (Object) this;
        options.resourcePacks.add("file/client.zip");
        options.incompatibleResourcePacks.add("file/client.zip");
    }

}
