package net.hollowcube.terraform.compat.vanilla;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.vanilla.command.SetBlockCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class VanillaModule implements TerraformModule {

    @Override
    public @NotNull Set<CommandDsl> commands() {
        return Set.of(
                new SetBlockCommand()
        );
    }
}
