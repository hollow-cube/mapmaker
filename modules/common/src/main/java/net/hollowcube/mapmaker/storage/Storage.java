package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.error.Error;

public interface Storage {

    Error ERR_NOT_FOUND = Error.of("not found");
    Error ERR_DUPLICATE_ENTRY = Error.of("already exists");

}
