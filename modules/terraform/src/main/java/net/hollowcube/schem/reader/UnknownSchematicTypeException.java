package net.hollowcube.schem.reader;

public class UnknownSchematicTypeException extends RuntimeException {
    public UnknownSchematicTypeException() {
        super("unable to determine schematic type");
    }
}
