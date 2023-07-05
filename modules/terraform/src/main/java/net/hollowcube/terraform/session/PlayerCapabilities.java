package net.hollowcube.terraform.session;

/**
 * Player capabilities contains information about the capabilities/limits of a player.
 * Configurations are not player specific, so may be reused between players.
 */
public record PlayerCapabilities(
        boolean canIgnoreWorldBorder
) {
}
