package net.hollowcube.terraform.compat;

import net.hollowcube.terraform.compat.arceon.TerraformArceon;
import net.hollowcube.terraform.compat.worldedit.TerraformWorldEdit;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TerraformCompat {
    private TerraformCompat() {}

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        TerraformWorldEdit.init(eventNode, condition);
        TerraformArceon.init(eventNode, condition);
    }

}
