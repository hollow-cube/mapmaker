package net.hollowcube.mapmaker.util;

public abstract class AbstractMemoryService {
    protected static final boolean SLOW = "1".equals(System.getenv("MAPMAKER_SIMULATED_SLOWNESS"));
}
