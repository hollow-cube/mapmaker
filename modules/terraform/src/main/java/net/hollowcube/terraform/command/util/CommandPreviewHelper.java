package net.hollowcube.terraform.command.util;

import net.hollowcube.terraform.cui.ClientRenderer;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;

public class CommandPreviewHelper {

    public static final Tag<Task> DEBOUNCE_TASK = Tag.Transient("mapmaker:terraform_command_debounce");


    public static void debounceContext(@NotNull Player player, @NotNull ClientRenderer renderer) {
        player.updateTag(DEBOUNCE_TASK, task -> {
            if (task != null) {
                task.cancel();
            }

            return player.scheduler()
                    .buildTask(() -> renderer.switchTo(ClientRenderer.RenderContext.NORMAL, false))
                    .delay(5, ChronoUnit.SECONDS)
                    .schedule();
        });
    }
}
