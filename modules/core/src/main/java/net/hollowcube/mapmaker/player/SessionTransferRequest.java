package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.NotNull;

public record SessionTransferRequest(
        @NotNull String server
) {
}
