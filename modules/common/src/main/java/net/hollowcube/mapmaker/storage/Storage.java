package net.hollowcube.mapmaker.storage;

public interface Storage {
    Throwable NOT_FOUND = new Throwable("Map not found");
    Throwable DUPLICATE_ENTRY = new Throwable("Map already exists");
}
