package net.hollowcube.mapmaker.model;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MapData {

    private String id;
    private String owner;
    private String name;

    // ID of the file in storage, or null if the map does not yet exist (it is lazily created)
    private String mapFileId;
    private final List<POI> pois = new ArrayList<>();

    private final int MAX_COMPLETION_TIMES = 10;
    private final List<CompletionTime> completionTimes = new ArrayList<>(MAX_COMPLETION_TIMES + 1);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMapFileId() {
        return mapFileId;
    }

    public void setMapFileId(String mapFileId) {
        this.mapFileId = mapFileId;
    }

    public List<POI> getPois() {
        return pois;
    }

    public void addPOI(@NotNull POI poi) {
        pois.add(poi);
    }

    public void removePOI(@NotNull Point pos) {
        pois.removeIf(poi -> poi.pos.equals(pos));
    }

    public void tryAddTime(UUID id, long time) {
        int index = -1;
        for (int i = 0; i < completionTimes.size(); i++) {
            // Find index
            if (time < completionTimes.get(i).timeInMills) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            // We were not able to find an index, could be in a couple of states, array is empty, it's the last possible time, etc
            // Add onto end, we will correct it later
            completionTimes.add(new CompletionTime(id, time));
        } else {
            // found insertion index
            completionTimes.add(index, new CompletionTime(id, time));
        }
        // Remove times until we are at the max to correct data
        while (completionTimes.size() > MAX_COMPLETION_TIMES) {
            completionTimes.remove(completionTimes.size() - 1);
        }
    }

    public List<CompletionTime> getCompletionTimes() {
        return completionTimes;
    }

    @Override
    public String toString() {
        return "MapData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    public record POI(String type, Point pos) {}

    public record CompletionTime(UUID playerUUID, long timeInMills) {}

}
