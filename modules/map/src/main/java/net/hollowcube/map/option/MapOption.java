package net.hollowcube.map.option;

import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all map options. Options are loaded during startup using SPI.
 */
public interface MapOption {

    /** Returns the ID of the option, should only contain alphanumerics and underscores. */
    @NotNull String id();

}
