package net.hollowcube.ipc.map;

import java.util.List;

public record PublishReadiness(
    List<PublishRequirement> missing,
    long buildTime,
    long requiredBuildTime
) {
    public boolean ready() {
        return missing.isEmpty();
    }
}
