package net.hollowcube.schem.writer;

public class SchematicWriteException extends RuntimeException {

    SchematicWriteException(String message) {
        super(message);
    }

    SchematicWriteException(String message, Throwable cause) {
        super(message, cause);
    }

    SchematicWriteException(Throwable cause) {
        super(cause);
    }
}
